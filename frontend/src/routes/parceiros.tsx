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
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
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
  type PartnerClient,
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
  CUSTOM_API: "API própria",
};

function SpecialistDialog({ open, onClose }: { open: boolean; onClose: () => void }) {
  const queryClient = useQueryClient();
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!open) return;
    setName("");
    setEmail("");
    setError(null);
  }, [open]);

  const mutation = useMutation({
    mutationFn: () => invitePartnerSpecialist({ name: name.trim(), email: email.trim() }),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["partner-specialists"] }),
        queryClient.invalidateQueries({ queryKey: ["team"] }),
      ]);
      onClose();
    },
    onError: (cause: Error) => setError(cause.message),
  });

  const canSubmit = name.trim().length > 0 && email.includes("@") && !mutation.isPending;

  return (
    <Dialog open={open} onOpenChange={(value) => !value && onClose()}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Convidar especialista</DialogTitle>
          <DialogDescription>
            O especialista receberá acesso para criar e editar avaliações no ambiente do parceiro.
          </DialogDescription>
        </DialogHeader>
        <div className="space-y-4 py-2">
          <div className="space-y-1.5">
            <Label htmlFor="specialist-name">Nome</Label>
            <Input id="specialist-name" value={name} onChange={(event) => setName(event.target.value)} />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="specialist-email">E-mail</Label>
            <Input
              id="specialist-email"
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
            />
          </div>
          {error && <p className="text-sm text-destructive">{error}</p>}
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={onClose} disabled={mutation.isPending}>Cancelar</Button>
          <Button onClick={() => mutation.mutate()} disabled={!canSubmit}>
            {mutation.isPending ? "Enviando..." : "Enviar convite"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

function ClientDialog({ open, onClose }: { open: boolean; onClose: () => void }) {
  const queryClient = useQueryClient();
  const [name, setName] = useState("");
  const [externalCompanyId, setExternalCompanyId] = useState("");
  const [provider, setProvider] = useState<PartnerProvider>("GUPY");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!open) return;
    setName("");
    setExternalCompanyId("");
    setProvider("GUPY");
    setError(null);
  }, [open]);

  const mutation = useMutation({
    mutationFn: () => createPartnerClient({
      name: name.trim(),
      externalCompanyId: externalCompanyId.trim(),
      provider,
    }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["partner-clients"] });
      onClose();
    },
    onError: (cause: Error) => setError(cause.message),
  });

  const canSubmit = name.trim().length > 0 && externalCompanyId.trim().length > 0 && !mutation.isPending;

  return (
    <Dialog open={open} onOpenChange={(value) => !value && onClose()}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Cadastrar cliente</DialogTitle>
          <DialogDescription>
            Use o identificador da empresa no sistema integrado. Ele será validado ao registrar candidatos.
          </DialogDescription>
        </DialogHeader>
        <div className="space-y-4 py-2">
          <div className="space-y-1.5">
            <Label htmlFor="client-name">Nome do cliente</Label>
            <Input id="client-name" value={name} onChange={(event) => setName(event.target.value)} />
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
          {error && <p className="text-sm text-destructive">{error}</p>}
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={onClose} disabled={mutation.isPending}>Cancelar</Button>
          <Button onClick={() => mutation.mutate()} disabled={!canSubmit}>
            {mutation.isPending ? "Salvando..." : "Cadastrar"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

function TokenDialog({ token, onClose }: { token: PartnerClientToken | null; onClose: () => void }) {
  const [copied, setCopied] = useState(false);

  useEffect(() => setCopied(false), [token]);

  async function copyToken() {
    if (!token) return;
    await navigator.clipboard.writeText(token.token);
    setCopied(true);
  }

  return (
    <Dialog open={token !== null} onOpenChange={(value) => !value && onClose()}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>Token gerado</DialogTitle>
          <DialogDescription>
            Copie agora. Por segurança, o valor completo não será mostrado novamente.
          </DialogDescription>
        </DialogHeader>
        <div className="rounded-md border border-border bg-muted/40 p-3 font-mono text-xs break-all">
          {token?.token}
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={onClose}>Fechar</Button>
          <Button onClick={copyToken}>
            {copied ? <CheckCircle2 className="mr-2 h-4 w-4" /> : <Copy className="mr-2 h-4 w-4" />}
            {copied ? "Copiado" : "Copiar token"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

function PartnersPage() {
  const queryClient = useQueryClient();
  const [specialistDialogOpen, setSpecialistDialogOpen] = useState(false);
  const [clientDialogOpen, setClientDialogOpen] = useState(false);
  const [selectedClientId, setSelectedClientId] = useState("");
  const [selectedSimulationIds, setSelectedSimulationIds] = useState<Set<string>>(new Set());
  const [generatedToken, setGeneratedToken] = useState<PartnerClientToken | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const specialistsQuery = useQuery({
    queryKey: ["partner-specialists"],
    queryFn: listPartnerSpecialists,
  });
  const clientsQuery = useQuery({ queryKey: ["partner-clients"], queryFn: listPartnerClients });
  const clients = clientsQuery.data ?? [];

  useEffect(() => {
    if (!selectedClientId && clients.length > 0) setSelectedClientId(clients[0].id);
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
    setSelectedSimulationIds(new Set(
      catalogQuery.data.filter((item) => item.assigned).map((item) => item.simulationId),
    ));
  }, [catalogQuery.data]);

  const removeSpecialistMutation = useMutation({
    mutationFn: removePartnerSpecialist,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["partner-specialists"] }),
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
      await queryClient.invalidateQueries({ queryKey: ["partner-clients"] });
    },
    onError: (cause: Error) => setActionError(cause.message),
  });
  const catalogMutation = useMutation({
    mutationFn: () => updatePartnerClientCatalog(
      selectedClientId,
      Array.from(selectedSimulationIds),
    ),
    onSuccess: async () => {
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

  return (
    <AppShell>
      <div className="mx-auto max-w-6xl space-y-8">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <h1 className="text-2xl font-semibold tracking-tight text-foreground">Parceiros e especialistas</h1>
            <p className="mt-1 max-w-2xl text-sm text-muted-foreground">
              Organize quem produz avaliações e quais testes cada cliente poderá usar pela integração.
            </p>
          </div>
          <div className="flex flex-wrap gap-2">
            <Button variant="outline" asChild>
              <Link to="/nova/avaliacao"><Plus className="mr-2 h-4 w-4" />Criar avaliação</Link>
            </Button>
            <Button onClick={() => setSpecialistDialogOpen(true)}>
              <UserPlus className="mr-2 h-4 w-4" />Convidar especialista
            </Button>
          </div>
        </div>

        <SpecialistDialog open={specialistDialogOpen} onClose={() => setSpecialistDialogOpen(false)} />
        <ClientDialog open={clientDialogOpen} onClose={() => setClientDialogOpen(false)} />
        <TokenDialog token={generatedToken} onClose={() => setGeneratedToken(null)} />

        {actionError && (
          <div className="rounded-lg border border-destructive/30 bg-destructive/10 p-4 text-sm text-destructive">
            {actionError}
          </div>
        )}

        <section className="space-y-3">
          <div className="flex items-center justify-between gap-3">
            <div>
              <h2 className="flex items-center gap-2 text-lg font-semibold"><Users className="h-5 w-5" />Especialistas</h2>
              <p className="text-sm text-muted-foreground">Especialistas usam o editor atual do Práxis para criar os testes.</p>
            </div>
          </div>
          {specialistsQuery.isLoading ? (
            <div className="h-24 animate-pulse rounded-lg bg-muted" />
          ) : specialistsQuery.isError ? (
            <Button variant="outline" onClick={() => specialistsQuery.refetch()}><RefreshCw className="mr-2 h-4 w-4" />Tentar novamente</Button>
          ) : (specialistsQuery.data?.length ?? 0) === 0 ? (
            <div className="rounded-lg border border-dashed border-border p-8 text-center text-sm text-muted-foreground">
              Nenhum especialista cadastrado.
            </div>
          ) : (
            <ResponsiveTable minWidth="680px">
              <table className="w-full text-sm">
                <thead className="bg-muted/50 text-xs uppercase text-muted-foreground">
                  <tr><th className="px-4 py-3 text-left">Nome</th><th className="px-4 py-3 text-left">E-mail</th><th className="px-4 py-3 text-left">Status</th><th className="px-4 py-3 text-right">Ação</th></tr>
                </thead>
                <tbody className="divide-y divide-border">
                  {specialistsQuery.data?.map((specialist) => (
                    <tr key={specialist.id}>
                      <td className="px-4 py-3 font-medium">{specialist.name}</td>
                      <td className="px-4 py-3 text-muted-foreground">{specialist.email}</td>
                      <td className="px-4 py-3">{specialist.status}</td>
                      <td className="px-4 py-3 text-right"><Button size="sm" variant="outline" onClick={() => removeSpecialistMutation.mutate(specialist.id)}>Remover função</Button></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </ResponsiveTable>
          )}
        </section>

        <section className="space-y-3">
          <div className="flex items-center justify-between gap-3">
            <div>
              <h2 className="flex items-center gap-2 text-lg font-semibold"><BriefcaseBusiness className="h-5 w-5" />Clientes do parceiro</h2>
              <p className="text-sm text-muted-foreground">A cobrança permanece fora deste módulo.</p>
            </div>
            <Button variant="outline" onClick={() => setClientDialogOpen(true)}><Plus className="mr-2 h-4 w-4" />Cadastrar cliente</Button>
          </div>
          {clientsQuery.isLoading ? (
            <div className="h-28 animate-pulse rounded-lg bg-muted" />
          ) : clients.length === 0 ? (
            <div className="rounded-lg border border-dashed border-border p-8 text-center text-sm text-muted-foreground">Nenhum cliente cadastrado.</div>
          ) : (
            <ResponsiveTable minWidth="860px">
              <table className="w-full text-sm">
                <thead className="bg-muted/50 text-xs uppercase text-muted-foreground">
                  <tr><th className="px-4 py-3 text-left">Cliente</th><th className="px-4 py-3 text-left">Integração</th><th className="px-4 py-3 text-left">Catálogo</th><th className="px-4 py-3 text-left">Token</th><th className="px-4 py-3 text-right">Ações</th></tr>
                </thead>
                <tbody className="divide-y divide-border">
                  {clients.map((client) => (
                    <tr key={client.id} className={client.id === selectedClientId ? "bg-muted/30" : undefined}>
                      <td className="px-4 py-3"><button type="button" className="text-left font-medium hover:underline" onClick={() => setSelectedClientId(client.id)}>{client.name}</button><div className="text-xs text-muted-foreground">ID: {client.externalCompanyId}</div></td>
                      <td className="px-4 py-3">{providerLabels[client.provider]}<div className="text-xs text-muted-foreground">{client.active ? "Ativo" : "Inativo"}</div></td>
                      <td className="px-4 py-3">{client.assignedTests} teste(s)</td>
                      <td className="px-4 py-3">{client.tokenConfigured ? "Configurado" : "Não gerado"}</td>
                      <td className="px-4 py-3 text-right"><div className="flex justify-end gap-2"><Button size="sm" variant="outline" onClick={() => tokenMutation.mutate(client.id)} disabled={!client.active}><KeyRound className="mr-1 h-3.5 w-3.5" />Gerar token</Button><Button size="sm" variant="outline" onClick={() => activeMutation.mutate({ clientId: client.id, active: !client.active })}>{client.active ? "Desativar" : "Ativar"}</Button></div></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </ResponsiveTable>
          )}
        </section>

        <section className="space-y-3 rounded-xl border border-border bg-card p-5">
          <div>
            <h2 className="text-lg font-semibold">Catálogo liberado</h2>
            <p className="text-sm text-muted-foreground">
              {selectedClient ? `Selecione os testes disponíveis para ${selectedClient.name}.` : "Cadastre um cliente para configurar o catálogo."}
            </p>
          </div>
          {selectedClient && catalogQuery.isLoading && <div className="h-28 animate-pulse rounded-lg bg-muted" />}
          {selectedClient && catalogQuery.data && catalogQuery.data.length === 0 && (
            <p className="rounded-lg border border-dashed border-border p-6 text-sm text-muted-foreground">Publique uma avaliação antes de montar o catálogo.</p>
          )}
          {catalogQuery.data && catalogQuery.data.length > 0 && (
            <div className="space-y-3">
              <div className="grid gap-3 md:grid-cols-2">
                {catalogQuery.data.map((item) => (
                  <label key={item.simulationId} className="flex cursor-pointer gap-3 rounded-lg border border-border p-4 hover:bg-muted/30">
                    <input type="checkbox" className="mt-1 h-4 w-4" checked={selectedSimulationIds.has(item.simulationId)} onChange={() => toggleSimulation(item.simulationId)} disabled={!selectedClient?.active} />
                    <span><span className="block font-medium">{item.name}</span><span className="mt-1 block text-xs text-muted-foreground">{item.description}</span></span>
                  </label>
                ))}
              </div>
              <div className="flex justify-end"><Button onClick={() => catalogMutation.mutate()} disabled={!selectedClient?.active || catalogMutation.isPending}>{catalogMutation.isPending ? "Salvando..." : "Salvar catálogo"}</Button></div>
            </div>
          )}
        </section>
      </div>
    </AppShell>
  );
}
