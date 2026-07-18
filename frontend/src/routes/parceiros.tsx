import { createFileRoute, Link } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useMemo, useState } from "react";
import {
  BriefcaseBusiness,
  CheckCircle2,
  Copy,
  KeyRound,
  Plus,
  RefreshCw,
  UserPlus,
  Users,
} from "lucide-react";

import { AppShell } from "@/components/app-shell";
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
  });
  const clientsQuery = useQuery({
    queryKey: ["partner-clients"],
    queryFn: listPartnerClients,
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
  });

  useEffect(() => {
    if (!catalogQuery.data) return;
    setSelectedSimulationIds(
      new Set(
        catalogQuery.data
          .filter((item) => item.assigned)
          .map((item) => item.simulationId),
      ),
    );
  }, [catalogQuery.data]);

  const specialistMutation = useMutation({
    mutationFn: () =>
      invitePartnerSpecialist({
        name: specialistName.trim(),
        email: specialistEmail.trim(),
      }),
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
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["partner-specialists"] }),
    onError: (cause: Error) => setActionError(cause.message),
  });

  const clientMutation = useMutation({
    mutationFn: () =>
      createPartnerClient({
        name: clientName.trim(),
        externalCompanyId: externalCompanyId.trim(),
        provider,
      }),
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
    mutationFn: ({ clientId, active }: { clientId: string; active: boolean }) =>
      setPartnerClientActive(clientId, active),
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
    mutationFn: () =>
      updatePartnerClientCatalog(selectedClientId, Array.from(selectedSimulationIds)),
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
      <div className="mx-auto max-w-6xl space-y-8">
        <header className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <h1 className="text-2xl font-semibold tracking-tight text-foreground">
              Parceiros e especialistas
            </h1>
            <p className="mt-1 max-w-2xl text-sm text-muted-foreground">
              Organize quem produz avaliações e quais testes cada cliente poderá usar pela integração.
            </p>
          </div>
          <Button variant="outline" asChild>
            <Link to="/nova/avaliacao">
              <Plus className="mr-2 h-4 w-4" />Criar avaliação
            </Link>
          </Button>
        </header>

        {actionError && (
          <div className="rounded-lg border border-destructive/30 bg-destructive/10 p-4 text-sm text-destructive">
            {actionError}
          </div>
        )}

        <section className="space-y-4 rounded-xl border border-border bg-card p-5">
          <div>
            <h2 className="flex items-center gap-2 text-lg font-semibold">
              <Users className="h-5 w-5" />Especialistas
            </h2>
            <p className="text-sm text-muted-foreground">
              O perfil de especialista cria e revisa rascunhos, sem acesso a clientes, cobrança ou publicação.
            </p>
          </div>

          <div className="grid gap-3 md:grid-cols-[1fr_1fr_auto] md:items-end">
            <div className="space-y-1.5">
              <Label htmlFor="specialist-name">Nome</Label>
              <Input
                id="specialist-name"
                value={specialistName}
                onChange={(event) => setSpecialistName(event.target.value)}
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="specialist-email">E-mail</Label>
              <Input
                id="specialist-email"
                type="email"
                value={specialistEmail}
                onChange={(event) => setSpecialistEmail(event.target.value)}
              />
            </div>
            <Button
              onClick={() => specialistMutation.mutate()}
              disabled={
                specialistMutation.isPending ||
                !specialistName.trim() ||
                !specialistEmail.includes("@")
              }
            >
              <UserPlus className="mr-2 h-4 w-4" />
              {specialistMutation.isPending ? "Enviando..." : "Convidar"}
            </Button>
          </div>

          {specialistsQuery.isLoading ? (
            <div className="h-20 animate-pulse rounded-lg bg-muted" />
          ) : specialistsQuery.isError ? (
            <Button variant="outline" onClick={() => specialistsQuery.refetch()}>
              <RefreshCw className="mr-2 h-4 w-4" />Tentar novamente
            </Button>
          ) : (specialistsQuery.data?.length ?? 0) === 0 ? (
            <p className="rounded-lg border border-dashed border-border p-6 text-center text-sm text-muted-foreground">
              Nenhum especialista cadastrado.
            </p>
          ) : (
            <ResponsiveTable minWidth="680px">
              <table className="w-full text-sm">
                <thead className="bg-muted/50 text-xs uppercase text-muted-foreground">
                  <tr>
                    <th className="px-4 py-3 text-left">Nome</th>
                    <th className="px-4 py-3 text-left">E-mail</th>
                    <th className="px-4 py-3 text-left">Status</th>
                    <th className="px-4 py-3 text-right">Ação</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border">
                  {specialistsQuery.data?.map((specialist) => (
                    <tr key={specialist.id}>
                      <td className="px-4 py-3 font-medium">{specialist.name}</td>
                      <td className="px-4 py-3 text-muted-foreground">{specialist.email}</td>
                      <td className="px-4 py-3">{specialist.status}</td>
                      <td className="px-4 py-3 text-right">
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => removeSpecialistMutation.mutate(specialist.id)}
                        >
                          Remover função
                        </Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </ResponsiveTable>
          )}
        </section>

        <section className="space-y-4 rounded-xl border border-border bg-card p-5">
          <div>
            <h2 className="flex items-center gap-2 text-lg font-semibold">
              <BriefcaseBusiness className="h-5 w-5" />Clientes do parceiro
            </h2>
            <p className="text-sm text-muted-foreground">
              A cobrança permanece fora deste módulo. Aqui ficam apenas acesso, catálogo e integração.
            </p>
          </div>

          <div className="grid gap-3 md:grid-cols-[1fr_180px_1fr_auto] md:items-end">
            <div className="space-y-1.5">
              <Label htmlFor="client-name">Nome do cliente</Label>
              <Input
                id="client-name"
                value={clientName}
                onChange={(event) => setClientName(event.target.value)}
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="client-provider">Plataforma</Label>
              <select
                id="client-provider"
                value={provider}
                onChange={(event) => setProvider(event.target.value as PartnerProvider)}
                className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
              >
                {Object.entries(providerLabels).map(([value, label]) => (
                  <option key={value} value={value}>{label}</option>
                ))}
              </select>
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="client-external-id">ID da empresa na plataforma</Label>
              <Input
                id="client-external-id"
                value={externalCompanyId}
                onChange={(event) => setExternalCompanyId(event.target.value)}
                placeholder="Ex.: 12345"
              />
            </div>
            <Button
              onClick={() => clientMutation.mutate()}
              disabled={
                clientMutation.isPending ||
                !clientName.trim() ||
                !externalCompanyId.trim()
              }
            >
              <Plus className="mr-2 h-4 w-4" />
              {clientMutation.isPending ? "Salvando..." : "Cadastrar"}
            </Button>
          </div>

          {clientsQuery.isLoading ? (
            <div className="h-28 animate-pulse rounded-lg bg-muted" />
          ) : clientsQuery.isError ? (
            <Button variant="outline" onClick={() => clientsQuery.refetch()}>
              <RefreshCw className="mr-2 h-4 w-4" />Tentar novamente
            </Button>
          ) : clients.length === 0 ? (
            <p className="rounded-lg border border-dashed border-border p-6 text-center text-sm text-muted-foreground">
              Nenhum cliente cadastrado.
            </p>
          ) : (
            <ResponsiveTable minWidth="880px">
              <table className="w-full text-sm">
                <thead className="bg-muted/50 text-xs uppercase text-muted-foreground">
                  <tr>
                    <th className="px-4 py-3 text-left">Cliente</th>
                    <th className="px-4 py-3 text-left">Integração</th>
                    <th className="px-4 py-3 text-left">Catálogo</th>
                    <th className="px-4 py-3 text-left">Token</th>
                    <th className="px-4 py-3 text-right">Ações</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border">
                  {clients.map((client) => (
                    <tr
                      key={client.id}
                      className={client.id === selectedClientId ? "bg-muted/30" : undefined}
                    >
                      <td className="px-4 py-3">
                        <button
                          type="button"
                          className="text-left font-medium hover:underline"
                          onClick={() => setSelectedClientId(client.id)}
                        >
                          {client.name}
                        </button>
                        <div className="text-xs text-muted-foreground">ID: {client.externalCompanyId}</div>
                      </td>
                      <td className="px-4 py-3">
                        {providerLabels[client.provider]}
                        <div className="text-xs text-muted-foreground">
                          {client.active ? "Ativo" : "Inativo"}
                        </div>
                      </td>
                      <td className="px-4 py-3">{client.assignedTests} teste(s)</td>
                      <td className="px-4 py-3">
                        {client.tokenConfigured ? "Configurado" : "Não gerado"}
                      </td>
                      <td className="px-4 py-3 text-right">
                        <div className="flex justify-end gap-2">
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => tokenMutation.mutate(client.id)}
                            disabled={!client.active}
                          >
                            <KeyRound className="mr-1 h-3.5 w-3.5" />Gerar token
                          </Button>
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() =>
                              activeMutation.mutate({ clientId: client.id, active: !client.active })
                            }
                          >
                            {client.active ? "Desativar" : "Ativar"}
                          </Button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </ResponsiveTable>
          )}

          {generatedToken && (
            <div className="space-y-3 rounded-lg border border-amber-300/50 bg-amber-50/50 p-4 dark:bg-amber-950/10">
              <div>
                <h3 className="font-medium">Token gerado</h3>
                <p className="text-xs text-muted-foreground">
                  Copie agora. O valor completo não será exibido novamente.
                </p>
              </div>
              <div className="break-all rounded-md border border-border bg-background p-3 font-mono text-xs">
                {generatedToken.token}
              </div>
              <Button size="sm" onClick={copyGeneratedToken}>
                {copied ? (
                  <CheckCircle2 className="mr-2 h-4 w-4" />
                ) : (
                  <Copy className="mr-2 h-4 w-4" />
                )}
                {copied ? "Copiado" : "Copiar token"}
              </Button>
            </div>
          )}
        </section>

        <section className="space-y-4 rounded-xl border border-border bg-card p-5">
          <div>
            <h2 className="text-lg font-semibold">Catálogo liberado</h2>
            <p className="text-sm text-muted-foreground">
              {selectedClient
                ? `Selecione os testes disponíveis para ${selectedClient.name}.`
                : "Cadastre um cliente para configurar o catálogo."}
            </p>
          </div>

          {selectedClient && catalogQuery.isLoading && (
            <div className="h-28 animate-pulse rounded-lg bg-muted" />
          )}
          {selectedClient && catalogQuery.isError && (
            <Button variant="outline" onClick={() => catalogQuery.refetch()}>
              <RefreshCw className="mr-2 h-4 w-4" />Tentar novamente
            </Button>
          )}
          {selectedClient && catalogQuery.data?.length === 0 && (
            <p className="rounded-lg border border-dashed border-border p-6 text-sm text-muted-foreground">
              Publique uma avaliação antes de montar o catálogo.
            </p>
          )}
          {catalogQuery.data && catalogQuery.data.length > 0 && (
            <div className="space-y-3">
              <div className="grid gap-3 md:grid-cols-2">
                {catalogQuery.data.map((item) => (
                  <label
                    key={item.simulationId}
                    className="flex cursor-pointer gap-3 rounded-lg border border-border p-4 hover:bg-muted/30"
                  >
                    <input
                      type="checkbox"
                      className="mt-1 h-4 w-4"
                      checked={selectedSimulationIds.has(item.simulationId)}
                      onChange={() => toggleSimulation(item.simulationId)}
                      disabled={!selectedClient?.active}
                    />
                    <span>
                      <span className="block font-medium">{item.name}</span>
                      <span className="mt-1 block text-xs text-muted-foreground">{item.description}</span>
                    </span>
                  </label>
                ))}
              </div>
              <div className="flex justify-end">
                <Button
                  onClick={() => catalogMutation.mutate()}
                  disabled={!selectedClient?.active || catalogMutation.isPending}
                >
                  {catalogMutation.isPending ? "Salvando..." : "Salvar catálogo"}
                </Button>
              </div>
            </div>
          )}
        </section>
      </div>
    </AppShell>
  );
}
