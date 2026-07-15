from pathlib import Path


def replace_exact(path: str, old: str, new: str, expected: int = 1) -> None:
    file = Path(path)
    text = file.read_text(encoding="utf-8")
    actual = text.count(old)
    if actual != expected:
        raise RuntimeError(f"{path}: esperado {expected} ocorrência(s), encontrado {actual}: {old!r}")
    file.write_text(text.replace(old, new), encoding="utf-8")


integration_test = "backend/src/test/java/br/com/iforce/praxis/gupy/controller/GupyIntegrationControllerTest.java"
replace_exact(
    integration_test,
    '                .andExpect(jsonPath("$.result_page_url").value(containsString("/test/result/" + resultId)))\n                .andExpect(jsonPath("$.result_page_url").value(not(containsString("?company_id="))))',
    '                .andExpect(jsonPath("$.result_page_url").value(containsString("/results/")))\n                .andExpect(jsonPath("$.result_page_url").value(not(containsString("/test/result/"))))\n                .andExpect(jsonPath("$.result_page_url").value(not(containsString("?company_id="))))',
)

contract_test = "backend/src/test/java/br/com/iforce/praxis/gupy/controller/GupyResultEndpointContractTest.java"
replace_exact(
    contract_test,
    '                .andExpect(jsonPath("$.result_page_url").value(containsString("/test/result/" + resultId)))\n                .andExpect(jsonPath("$.result_page_url").value(not(containsString("?company_id="))));',
    '                .andExpect(jsonPath("$.result_page_url").value(containsString("/results/")))\n                .andExpect(jsonPath("$.result_page_url").value(not(containsString("/test/result/"))))\n                .andExpect(jsonPath("$.result_page_url").value(not(containsString("?company_id="))));',
)

print("Expectativas das páginas reais atualizadas.")
