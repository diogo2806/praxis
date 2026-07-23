#!/usr/bin/env python3
"""Valida a governança e a sincronização da documentação do repositório."""

from __future__ import annotations

import re
import sys
from pathlib import Path
from urllib.parse import unquote, urlparse

ROOT = Path(__file__).resolve().parents[1]
CANONICAL_SOURCE = ROOT / "docs" / "GUPY-FONTE-CANONICA.md"
OPERACAO_DOC = ROOT / "docs" / "OPERACAO.md"
IMPLANTACAO_DOC = ROOT / "docs" / "IMPLANTACAO.md"
APPLICATION_PROPERTIES = ROOT / "backend" / "src" / "main" / "resources" / "application.properties"
FRONTEND_DOCKERFILE = ROOT / "frontend" / "Dockerfile"
PASSWORD_RESET_CONTROLLER = (
    ROOT
    / "backend"
    / "src"
    / "main"
    / "java"
    / "br"
    / "com"
    / "iforce"
    / "praxis"
    / "auth"
    / "controller"
    / "PasswordResetController.java"
)
GUPY_DOCS = (
    ROOT / "docs" / "00-INDICE.md",
    CANONICAL_SOURCE,
    ROOT / "docs" / "INTEGRACAO-GUPY-PROVEDOR.md",
    ROOT / "docs" / "HOMOLOGACAO-GUPY.md",
    ROOT / "docs" / "ARQUITETURA_OUTBOX_PATTERN.md",
    ROOT / "docs" / "IMPLEMENTATION_SUMMARY.md",
    ROOT / "docs" / "P0_PRODUCT_READINESS.md",
    ROOT / "docs" / "documentacao-tecnica.md",
)

MARKDOWN_LINK_RE = re.compile(r"!?\[[^\]]*]\(([^)]+)\)")
GUPY_URL_RE = re.compile(r"https://developers\.gupy\.io/[^\s)>]+", re.IGNORECASE)
SUSPICIOUS_UPSTREAM_RE = re.compile(
    r"(?:copy-of|(?:^|[-/])copy(?:[-/?#]|$)|/ntegra|(?:/|%2F)[^?#\s]*-(?:[?#]|$))",
    re.IGNORECASE,
)
CODE_FENCE_RE = re.compile(r"^\s*(```|~~~)")
PROPERTY_RE = re.compile(r"^([^#=\s]+)=(.*)$")


def markdown_files() -> list[Path]:
    ignored_parts = {".git", "node_modules", "target", "dist", "build", ".next"}
    return [
        path
        for path in ROOT.rglob("*.md")
        if not any(part in ignored_parts for part in path.parts)
    ]


def normalize_link_target(raw_target: str) -> str:
    target = raw_target.strip()
    if target.startswith("<") and target.endswith(">"):
        target = target[1:-1].strip()
    if " " in target and not target.startswith(("http://", "https://")):
        target = target.split(" ", 1)[0]
    return target


def count_markdown_h1(content: str) -> int:
    count = 0
    in_code_fence = False
    for line in content.splitlines():
        if CODE_FENCE_RE.match(line):
            in_code_fence = not in_code_fence
            continue
        if not in_code_fence and re.match(r"^#\s+\S", line):
            count += 1
    return count


def read_required(path: Path, errors: list[str]) -> str:
    if not path.is_file():
        errors.append(f"Arquivo obrigatório ausente: {path.relative_to(ROOT)}")
        return ""
    content = path.read_text(encoding="utf-8")
    if not content.strip():
        errors.append(f"Arquivo obrigatório vazio: {path.relative_to(ROOT)}")
    return content


def read_properties(errors: list[str]) -> dict[str, str]:
    content = read_required(APPLICATION_PROPERTIES, errors)
    properties: dict[str, str] = {}
    for line in content.splitlines():
        match = PROPERTY_RE.match(line.strip())
        if match:
            properties[match.group(1)] = match.group(2)
    return properties


def default_from_placeholder(value: str) -> str:
    match = re.fullmatch(r"\$\{[^:}]+:(.*)}", value)
    return match.group(1) if match else value


def validate_required_files(errors: list[str]) -> None:
    for path in (*GUPY_DOCS, OPERACAO_DOC, IMPLANTACAO_DOC):
        read_required(path, errors)


def validate_local_links(errors: list[str]) -> None:
    for source in GUPY_DOCS:
        if not source.is_file():
            continue
        content = source.read_text(encoding="utf-8")
        for match in MARKDOWN_LINK_RE.finditer(content):
            target = normalize_link_target(match.group(1))
            if not target or target.startswith(("#", "mailto:", "tel:", "data:", "javascript:")):
                continue
            parsed = urlparse(target)
            if parsed.scheme in {"http", "https"}:
                continue

            relative_path = unquote(parsed.path)
            if not relative_path:
                continue
            resolved = (source.parent / relative_path).resolve()
            try:
                resolved.relative_to(ROOT.resolve())
            except ValueError:
                errors.append(
                    f"Link sai do repositório em {source.relative_to(ROOT)}: {target}"
                )
                continue
            if not resolved.exists():
                errors.append(
                    f"Link local quebrado em {source.relative_to(ROOT)}: {target}"
                )


def validate_gupy_source_centralization(errors: list[str]) -> None:
    canonical_resolved = CANONICAL_SOURCE.resolve()
    for source in markdown_files():
        content = source.read_text(encoding="utf-8")
        for url in GUPY_URL_RE.findall(content):
            if source.resolve() != canonical_resolved:
                errors.append(
                    "Link direto para developers.gupy.io fora da fonte canônica: "
                    f"{source.relative_to(ROOT)} -> {url}"
                )
            if SUSPICIOUS_UPSTREAM_RE.search(url):
                errors.append(
                    f"URL Gupy provisória ou suspeita em {source.relative_to(ROOT)}: {url}"
                )


def validate_single_h1(errors: list[str]) -> None:
    for source in GUPY_DOCS:
        if not source.is_file():
            continue
        h1_count = count_markdown_h1(source.read_text(encoding="utf-8"))
        if h1_count != 1:
            errors.append(
                f"{source.relative_to(ROOT)} deve possuir exatamente um título H1; encontrado: {h1_count}"
            )


def require_text(content: str, expected: str, source: Path, errors: list[str]) -> None:
    if expected not in content:
        errors.append(
            f"Documentação dessincronizada em {source.relative_to(ROOT)}: ausente `{expected}`"
        )


def validate_operational_docs(errors: list[str]) -> None:
    operacao = read_required(OPERACAO_DOC, errors)
    implantacao = read_required(IMPLANTACAO_DOC, errors)
    properties = read_properties(errors)
    dockerfile = read_required(FRONTEND_DOCKERFILE, errors)
    password_reset = read_required(PASSWORD_RESET_CONTROLLER, errors)

    swagger_default = default_from_placeholder(
        properties.get("springdoc.swagger-ui.enabled", "")
    )
    actuator_default = default_from_placeholder(
        properties.get("management.endpoints.web.exposure.include", "")
    )

    require_text(
        operacao,
        f"`springdoc.swagger-ui.enabled` | `SPRINGDOC_SWAGGER_UI_ENABLED` | Expõe Swagger UI em `/docs` | `{swagger_default}`",
        OPERACAO_DOC,
        errors,
    )
    require_text(
        operacao,
        f"Por padrão, `{actuator_default}` são expostos via web",
        OPERACAO_DOC,
        errors,
    )

    if "/forgot" in password_reset and "/reset" in password_reset:
        require_text(operacao, "POST /api/v1/auth/password/forgot", OPERACAO_DOC, errors)
        require_text(operacao, "POST /api/v1/auth/password/reset", OPERACAO_DOC, errors)

    if 'CMD ["node", ".output/server/index.mjs"]' in dockerfile:
        require_text(
            operacao,
            "container Node.js 22 na porta `80`",
            OPERACAO_DOC,
            errors,
        )
        require_text(
            implantacao,
            "servidor Node.js gerado pelo TanStack Start na porta 80",
            IMPLANTACAO_DOC,
            errors,
        )

    require_text(operacao, "TEAM_MANAGER", OPERACAO_DOC, errors)
    require_text(operacao, "ASSESSMENT_EDITOR", OPERACAO_DOC, errors)
    require_text(operacao, "RESULTS_ANALYST", OPERACAO_DOC, errors)
    require_text(operacao, "OPERATIONS_MANAGER", OPERACAO_DOC, errors)
    require_text(operacao, "PARTNER_MANAGER", OPERACAO_DOC, errors)
    require_text(operacao, "PARTNER_SPECIALIST", OPERACAO_DOC, errors)

    if "V1..V47" in implantacao:
        errors.append(
            "Documentação dessincronizada em docs/IMPLANTACAO.md: intervalo fixo V1..V47"
        )
    require_text(
        implantacao,
        "sequência versionada atualmente acima de `V1000`",
        IMPLANTACAO_DOC,
        errors,
    )


def main() -> int:
    errors: list[str] = []
    validate_required_files(errors)
    validate_local_links(errors)
    validate_gupy_source_centralization(errors)
    validate_single_h1(errors)
    validate_operational_docs(errors)

    if errors:
        print("Falhas na documentação:")
        for error in errors:
            print(f" - {error}")
        return 1

    print("Documentação validada com sucesso.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
