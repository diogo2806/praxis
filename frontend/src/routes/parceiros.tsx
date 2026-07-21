import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createFileRoute, Link } from "@tanstack/react-router";
import { useEffect, useMemo, useState } from "react";
import {
  BriefcaseBusiness,
  CheckCircle2,
  Copy,
  KeyRound,
  LockKeyhole,
  Plus,
  RefreshCw,
  UserPlus,
  Users,
} from "lucide-react";

import { AppShell } from "@/components/app-shell";
import { StateBanner } from "@/components/praxis-ui";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { ResponsiveTable } from "@/components/ui/responsive-table";
import {
  createPartnerClient,
  invitePartnerSpecialist,
  listPartnerClientCatalog,
  listPartnerClients,
  listPartnerSpecialists,
  removePartnerSpecialist,
  rotatePartnerClientToken,
  setPartnerClientActive,
  updatePartnerClientCatalog,
  type PartnerClientToken,
  type PartnerProvider,
} from "@/lib/api/partners";
import { canManagePartners, isPartnerModuleEnabled } from "@/lib/feature-flags";
import { useSession } from "@/lib/session";

export const Route = createFileRoute("/parceiros")({
  head: () => ({
    meta: [
      { title: "Parceiros e especialistas - Práxis" },
      {
        name: "description",
        content: "Gerencie especialistas, clientes e o catálogo distribuído de avaliações.",
      },
    ],
  }),
  component: PartnersPage,
});

const providerLabels: Record<PartnerProvider, string> = {
  GUPY: "Gupy",
  RECRUTEI: "Recrutei",
};

function PartnersPage() {
  const session = useSession();
  const enabled = isPartnerModuleEnabled();
  const allowed = canManagePartners(session.roles);

  if (!allowed) {
    return (
      <AppShell>
        <main className="mx-auto max-w-2xl">
          <section className="rounded-xl border border-border bg-card p-8 text-center">
            <LockKeyhole className="mx-auto h-10 w-10 text-muted-foreground" />
            <h1 className="mt-4 text-2xl font-semibold">Módulo indisponível</h1>
            <p className="mx-auto mt-3 max-w-lg text-sm leading-6 text-muted-foreground">
              {enabled
                ? "O módulo está habilitado para a empresa, mas seu usuário não possui o perfil Administrador necessário para gerenciar parceiros."
                : "Parceiros e especialistas é um módulo comercial opcional e não está habilitado para esta empresa."}
            </p>
            <div className="mt-6 flex flex-wrap justify-center gap-3">
              <Link to="/dashboard" className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90">
                Voltar ao Dashboard
              </Link>
              <Link to="/team" className="rounded-md border border-border bg-background px-4 py-2 text-sm font-medium hover:bg-accent">
                Ver perfis da equipe
              </Link>
            </div>
          </section>
        </main>
      </AppShell>
    );
  }

  return <PartnersWorkspace />;
}

function PartnersWorkspace() {
  const queryClient = useQueryClient();
  const [specialistName, setSpecialistName] = useState("");
  const [specialistEmail, setSpecialistEmail] = useState("");
  const [clientName, setClientName] = useState("");
  const [externalCompanyId, setExternalCompanyId] = useState("");
  const [provider, setProvider] = useState<PartnerProvider>("GUPY");
  const [selectedClientId, setSelectedClientId] = useState("");
  const [selectedSimulationIds, setSelectedSimulationIds] = useState<Set<string>>(new Set());
  const [generatedToken, setGeneratedToken] = useState<PartnerClientToken | null>(null);
  const [copied, setCopied] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);

  const specialistsQuery = useQuery({
    queryKey: ["partner-specialists"],
    queryFn: listPartnerSpecialists,
    retry: false,
  });
  const clientsQuery = useQuery({
    queryKey: ["partner-clients"],
    queryFn: listPartnerClients,
    retry: false,
  });
  const clients = clientsQuery.data ?? [];

  useEffect(() => {
    if (!selectedClientId && clients.length > 0) {
      setSelectedClientId(clients[0].id);
      return;
    }
    if (selectedClientId && !clients.some((client) => client.id === selectedClientId)) {
      setSelectedClientId(clients[0]?.id ?? "");
    }
  }, [clients, selectedClientId]);

  const selectedClient = useMemo(
    () => clients.find((client) => client.id === selectedClientId) ?? null,
    [clients, selectedClientId],
  );

  const catalogQuery = useQuery({
    queryKey: ["partner-catalog", selectedClientId],
    queryFn: () => listPartnerClientCatalog(selectedClientId),
    enabled: selectedClientId.length > 0,
    retry: false,
  });

  useEffect(() => {
    if (!catalogQuery.data) return;
    setSelectedSimulationIds(
      new Set(catalogQuery.data.filter((item) => item.assigned).map((item) => item.simulationId)),
    );
  }, [catalogQuery.data]);

  const specialistMutation = useMutation({
    mutationFn: () => invitePartnerSpecialist({ name: specialistName.trim(), email: specialistEmail.trim() }),
    onSuccess: async () => {
      setSpecialistName("");
      setSpecialistEmail("");
      setActionError(null);
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["partner-specialists"] }),
        queryClient.invalidateQueries({ queryKey: ["team"] }),
      ]);
    },
    onError: (cause: Error) => setActionError(cause.message),
  });
  const removeSpecialistMutation = useMutation({
    mutationFn: removePartnerSpecialist,
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["partner-specialists"] }),
        queryClient.invalidateQueries({ queryKey: ["team"] }),
      ]);
    },
    onError: (cause: Error) => setActionError(cause.message),
  });
  const clientMutation = useMutation({
    mutationFn: () => createPartnerClient({ name: clientName.trim(), externalCompanyId: externalCompanyId.trim(), provider }),
    onSuccess: async (client) => {
      setClientName("");
      setExternalCompanyId("");
      setSelectedClientId(client.id);
      setActionError(null);
      await queryClient.invalidateQueries({ queryKey: ["partner-clients"] });
    },
    onError: (cause: Error) => setActionError(cause.message),
  });
  const activeMutation = useMutation({
    mutationFn: ({ clientId, active }: { clientId: string; active: boolean }) => setPartnerClientActive(clientId, active),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["partner-clients"] }),
    onError: (cause: Error) => setActionError(cause.message),
  });
  const tokenMutation = useMutation({
    mutationFn: rotatePartnerClientToken,
    onSuccess: async (token) => {
      setGeneratedToken(token);
      setCopied(false);
      setActionError(null);
      await queryClient.invalidateQueries({ queryKey: ["partner-clients"] });
    },
    onError: (cause: Error) => setActionError(cause.message),
  });
  const catalogMutation = useMutation({
    mutationFn: () => updatePartnerClientCatalog(selectedClientId, Array.from(selectedSimulationIds)),
    onSuccess: async () => {
      setActionError(null);
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["partner-catalog", selectedClientId] }),
        queryClient.invalidateQueries({ queryKey: ["partner-clients"] }),
      ]);
    },
    onError: (cause: Error) => setActionError(cause.message),
  });

  function toggleSimulation(simulationId: string) {
    setSelectedSimulationIds((current) => {
      const next = new Set(current);
      if (next.has(simulationId)) next.delete(simulationId);
      else next.add(simulationId);
      return next;
    });
  }

  async function copyGeneratedToken() {
    if (!generatedToken) return;
    await navigator.clipboard.writeText(generatedToken.token);
    setCopied(true);
  }

  return (
    <AppShell>
      <main className="mx-auto max-w-6xl space-y-8">
        <header className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <div className="text-xs font-semibold uppercase tracking-[0.18em] text-primary">Módulo condicional</div>
            <h1 className="mt-1 text-3xl font-semibold tracking-tight">Parceiros e especialistas</h1>
            <p className="mt-2 max-w-2xl text-sm text-muted-foreground">
              Disponível somente quando contratado e para usuários com perfil Administrador.
            </p>
          </div>
          <Button variant="outline" asChild>
            <Link to="/nova/avaliacao"><Plus className="mr-2 h-4 w-4" />Criar avaliação</Link>
          </Button>
        </header>

        {actionError && <StateBanner tone="danger" title="A ação não foi concluída">{actionError}</StateBanner>}

        <section className="space-y-4 rounded-xl border border-border bg-card p-5">
          <div>
            <h2 className="flex items-center gap-2 text-lg font-semibold"><Users className="h-5 w-5" />Especialistas</h2>
            <p className="text-sm text-muted-foreground">Especialistas criam e revisam rascunhos, sem administrar clientes, cobrança ou publicação.</p>
          </div>
          <div className="grid gap-3 md:grid-cols-[1fr_1fr_auto] md:items-end">
            <div className="space-y-1.5"><Label htmlFor="specialist-name">Nome</Label><Input id="specialist-name" value={specialistName} onChange={(event) => setSpecialistName(event.target.value)} /></div>
            <div className="space-y-1.5"><Label htmlFor="specialist-email">E-mail</Label><Input id="specialist-email" type="email" value={specialistEmail} onChange={(event) => setSpecialistEmail(event.target.value)} /></div>
            <Button onClick={() => specialistMutation.mutate()} disabled={specialistMutation.isPending || !specialistName.trim() || !specialistEmail.includes("@")}>
              <UserPlus className="mr-2 h-4 w-4" />{specialistMutation.isPending ? "Enviando..." : "Convidar"}
            </Button>
          </div>
          {specialistsQuery.isLoading ? <div className="h-20 animate-pulse rounded-lg bg-muted" /> : specialistsQuery.isError ? (
            <Button variant="outline" onClick={() => specialistsQuery.refetch()}><RefreshCw className="mr-2 h-4 w-4" />Tentar novamente</Button>
          ) : (specialistsQuery.data?.length ?? 0) === 0 ? (
            <p className="rounded-lg border border-dashed border-border p-6 text-center text-sm text-muted-foreground">Nenhum especialista cadastrado.</p>
          ) : (
            <ResponsiveTable minWidth="680px"><table className="w-full text-sm"><thead className="bg-muted/50 text-xs uppercase text-muted-foreground"><tr><th className="px-4 py-3 text-left">Nome</th><th className="px-4 py-3 text-left">E-mail</th><th className="px-4 py-3 text-left">Acesso</th><th className="px-4 py-3 text-right">Ação</th></tr></thead><tbody className="divide-y divide-border">{specialistsQuery.data?.map((specialist) => <tr key={specialist.id}><td className="px-4 py-3 font-medium">{specialist.name}</td><td className="px-4 py-3 text-muted-foreground">{specialist.email}</td><td className="px-4 py-3">{specialist.status}</td><td className="px-4 py-3 text-right"><Button size="sm" variant="outline" onClick={() => removeSpecialistMutation.mutate(specialist.id)} disabled={removeSpecialistMutation.isPending}>Remover função</Button></td></tr>)}</tbody></table></ResponsiveTable>
          )}
        </section>

        <section className="space-y-4 rounded-xl border border-border bg-card p-5">
          <div><h2 className="flex items-center gap-2 text-lg font-semibold"><BriefcaseBusiness className="h-5 w-5" />Clientes do parceiro</h2><p className="text-sm text-muted-foreground">Cadastre o cliente, libere o catálogo e gere a credencial de integração.</p></div>
          <div className="grid gap-3 md:grid-cols-[1fr_180px_1fr_auto] md:items-end">
            <div className="space-y-1.5"><Label htmlFor="client-name">Nome do cliente</Label><Input id="client-name" value={clientName} onChange={(event) => setClientName(event.target.value)} /></div>
            <div className="space-y-1.5"><Label htmlFor="client-provider">Plataforma</Label><select id="client-provider" value={provider} onChange={(event) => setProvider(event.target.value as PartnerProvider)} className="input w-full">{Object.entries(providerLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></div>
            <div className="space-y-1.5"><Label htmlFor="client-external-id">ID externo</Label><Input id="client-external-id" value={externalCompanyId} onChange={(event) => setExternalCompanyId(event.target.value)} placeholder="Ex.: 12345" /></div>
            <Button onClick={() => clientMutation.mutate()} disabled={clientMutation.isPending || !clientName.trim() || !externalCompanyId.trim()}><Plus className="mr-2 h-4 w-4" />{clientMutation.isPending ? "Salvando..." : "Adicionar"}</Button>
          </div>

          {clientsQuery.isLoading ? <div className="h-24 animate-pulse rounded-lg bg-muted" /> : clientsQuery.isError ? (
            <Button variant="outline" onClick={() => clientsQuery.refetch()}><RefreshCw className="mr-2 h-4 w-4" />Tentar novamente</Button>
          ) : clients.length === 0 ? (
            <p className="rounded-lg border border-dashed border-border p-6 text-center text-sm text-muted-foreground">Nenhum cliente cadastrado.</p>
          ) : (
            <ResponsiveTable minWidth="900px"><table className="w-full text-sm"><thead className="bg-muted/50 text-xs uppercase text-muted-foreground"><tr><th className="px-4 py-3 text-left">Cliente</th><th className="px-4 py-3 text-left">Plataforma</th><th className="px-4 py-3 text-left">Catálogo</th><th className="px-4 py-3 text-left">Token</th><th className="px-4 py-3 text-right">Ações</th></tr></thead><tbody className="divide-y divide-border">{clients.map((client) => <tr key={client.id}><td className="px-4 py-3"><div className="font-medium">{client.name}</div><div className="text-xs text-muted-foreground">{client.externalCompanyId} · {client.active ? "Ativo" : "Inativo"}</div></td><td className="px-4 py-3">{providerLabels[client.provider]}</td><td className="px-4 py-3">{client.assignedTests} teste{client.assignedTests === 1 ? "" : "s"}</td><td className="px-4 py-3">{client.tokenConfigured ? "Configurado" : "Não gerado"}</td><td className="px-4 py-3 text-right"><div className="flex flex-wrap justify-end gap-2"><Button size="sm" variant="outline" onClick={() => setSelectedClientId(client.id)}>Catálogo</Button><Button size="sm" variant="outline" onClick={() => tokenMutation.mutate(client.id)} disabled={!client.active || tokenMutation.isPending}><KeyRound className="mr-1 h-3.5 w-3.5" />Token</Button><Button size="sm" variant="outline" onClick={() => activeMutation.mutate({ clientId: client.id, active: !client.active })} disabled={activeMutation.isPending}>{client.active ? "Desativar" : "Ativar"}</Button></div></td></tr>)}</tbody></table></ResponsiveTable>
          )}
        </section>

        {selectedClient && (
          <section className="space-y-4 rounded-xl border border-border bg-card p-5">
            <div><h2 className="text-lg font-semibold">Catálogo de {selectedClient.name}</h2><p className="text-sm text-muted-foreground">Somente avaliações publicadas e marcadas ficam disponíveis para este cliente.</p></div>
            {catalogQuery.isLoading ? <div className="h-24 animate-pulse rounded-lg bg-muted" /> : catalogQuery.isError ? <StateBanner tone="danger" title="Não foi possível carregar o catálogo">Tente novamente.</StateBanner> : (
              <div className="grid gap-3 md:grid-cols-2">{catalogQuery.data?.map((item) => <label key={item.simulationId} className="flex cursor-pointer items-start gap-3 rounded-lg border border-border bg-background p-4"><input type="checkbox" className="mt-1" checked={selectedSimulationIds.has(item.simulationId)} onChange={() => toggleSimulation(item.simulationId)} disabled={!selectedClient.active} /><span><span className="block font-medium">{item.name}</span><span className="mt-1 block text-xs leading-5 text-muted-foreground">{item.description || "Sem descrição."}</span></span></label>)}</div>
            )}
            <Button onClick={() => catalogMutation.mutate()} disabled={!selectedClient.active || catalogMutation.isPending}>{catalogMutation.isPending ? "Salvando..." : "Salvar catálogo"}</Button>
          </section>
        )}

        {generatedToken && (
          <section className="rounded-xl border border-warning/40 bg-warning/5 p-5">
            <div className="flex items-start gap-3"><KeyRound className="mt-0.5 h-5 w-5 text-warning-foreground" /><div className="min-w-0 flex-1"><h2 className="font-semibold">Token gerado para {generatedToken.clientId}</h2><p className="mt-1 text-sm text-muted-foreground">Copie agora. O valor completo não será exibido novamente.</p><div className="mt-4 flex flex-col gap-2 sm:flex-row"><code className="min-w-0 flex-1 overflow-x-auto rounded-md border border-border bg-background p-3 text-xs">{generatedToken.token}</code><Button variant="outline" onClick={() => void copyGeneratedToken()}>{copied ? <CheckCircle2 className="mr-2 h-4 w-4" /> : <Copy className="mr-2 h-4 w-4" />}{copied ? "Copiado" : "Copiar"}</Button></div></div></div>
          </section>
        )}
      </main>
    </AppShell>
  );
}
