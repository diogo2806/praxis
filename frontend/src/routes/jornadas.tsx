var Pe=Object.defineProperty;var i=(e,s)=>Pe(e,"name",{value:s,configurable:!0});import{useMutation as K,useQuery as ae,useQueryClient as se}from"@tanstack/react-query";import{createFileRoute as je,Link as U}from"@tanstack/react-router";import{Archive as De,CheckCircle2 as Re,Edit3 as Be,Plus as Ne,RefreshCw as Ee,Save as Ie,Send as Ke,Trash2 as Le,Workflow as Fe,X as Me}from"lucide-react";import{useEffect as Qe,useMemo as Te,useState as w}from"react";import{AppShell as Oe}from"@/components/app-shell";import{EmptyState as Ce,SkeletonRows as Se,StateBanner as Ae}from"@/components/praxis-ui";import{AlertDialog as _e,AlertDialogAction as $e,AlertDialogCancel as Ve,AlertDialogContent as He,AlertDialogDescription as Ue,AlertDialogFooter as We,AlertDialogHeader as Xe,AlertDialogTitle as Ge}from"@/components/ui/alert-dialog";import{Button as N}from"@/components/ui/button";import{addAssessmentJourneyStep as Ye,archiveAssessmentJourney as Ze,createAssessmentJourney as ea,deleteAssessmentJourneyStep as aa,getAssessmentJourney as ta,listAssessmentJourneys as sa,listSimulations as ia,publishAssessmentJourney as na,updateAssessmentJourney as oa,updateAssessmentJourneyStep as ra}from"@/lib/api/praxis";import{cn as we}from"@/lib/utils";const Ka=je("/jornadas")({head:i(()=>({meta:[{title:"Jornadas de avalia\xE7\xE3o - Pr\xE1xis"},{name:"description",content:"Monte, ordene e publique sequ\xEAncias de avalia\xE7\xF5es."}]}),"head"),component:la}),te="principal";function la(){const e=se(),[s,x]=w(null),[j,k]=w(""),[q,D]=w(""),[T,$]=w(""),[V,W]=w(!0),[O,X]=w(te),[n,J]=w(null),L=ae({queryKey:["assessment-journeys"],queryFn:sa,retry:!1}),R=ae({queryKey:["assessment-journey",s],queryFn:i(()=>ta(s),"queryFn"),enabled:!!s,retry:!1}),F=ae({queryKey:["simulations"],queryFn:ia,retry:!1}),I=L.data??[],C=R.data??null,H=Te(()=>(F.data??[]).filter(t=>t.status==="published"||t.livePublishedVersionNumber!=null),[F.data]),z=K({mutationFn:ea,onSuccess:i(async t=>{k(""),D(""),x(t.id),await e.invalidateQueries({queryKey:["assessment-journeys"]})},"onSuccess")}),M=K({mutationFn:i(()=>Ye(s,{simulationId:T,sequenceKey:O.trim()||te,required:V}),"mutationFn"),onSuccess:i(async t=>{$(""),x(t.id),await Q(e,t.id)},"onSuccess")}),P=K({mutationFn:na,onSuccess:i(async t=>{J(null),await Q(e,t.id)},"onSuccess")}),A=K({mutationFn:Ze,onSuccess:i(async t=>{J(null),await Q(e,t.id)},"onSuccess")}),B=K({mutationFn:i(t=>aa(s,t),"mutationFn"),onSuccess:i(async()=>{J(null),await Q(e,s)},"onSuccess")}),E=K({mutationFn:i(({step:t,direction:ee})=>ra(s,t.id,{orderIndex:t.orderIndex+ee}),"mutationFn"),onSuccess:i(async()=>Q(e,s),"onSuccess")}),_=z.error??M.error??P.error??A.error??B.error??E.error,G=n?.action==="publish"?P.isPending:n?.action==="archive"?A.isPending:n?.action==="remove-step"?B.isPending:!1,Y=n?.action==="publish"?P.error:n?.action==="archive"?A.error:n?.action==="remove-step"?B.error:null;function Z(){j.trim()&&z.mutate({name:j.trim(),description:q.trim()||null})}i(Z,"createJourney");function a(){!s||!T||!O.trim()||M.mutate()}i(a,"addStep");function S(){if(n){if(n.action==="publish"){P.mutate(n.journeyId);return}if(n.action==="archive"){A.mutate(n.journeyId);return}B.mutate(n.stepId)}}return i(S,"confirmAction"),<Oe>
      <main className="mx-auto max-w-7xl space-y-6">
        <header className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <div className="text-xs font-semibold uppercase text-primary">Jornadas de avaliação</div>
            <h1 className="mt-1 text-3xl font-semibold">Composição das jornadas</h1>
            <p className="mt-2 max-w-3xl text-sm leading-6 text-muted-foreground">
              Organize avaliações publicadas em sequências. Convites, validade e acompanhamento ficam
              centralizados em Participações.
            </p>
          </div>
          <div className="flex flex-wrap gap-2">
            <N asChild className="gap-2">
              <U to="/participacoes/jornada">
                <Ke className="h-4 w-4"/>
                Criar convite
              </U>
            </N>
            <N type="button"variant="outline"className="gap-2 bg-card"onClick={()=>{L.refetch(),R.refetch(),F.refetch()}}>
              <Ee className="h-4 w-4"/>
              Atualizar
            </N>
          </div>
        </header>

        {_&&<Ae tone="danger"title="Não foi possível concluir a ação">
            {_ instanceof Error?_.message:"Tente novamente."}
          </Ae>}

        <div className="grid gap-5 lg:grid-cols-[300px_minmax(0,1fr)]">
          <aside className="space-y-5">
            <section className="rounded-xl border border-border bg-card p-4">
              <h2 className="text-sm font-semibold">Nova jornada</h2>
              <div className="mt-4 space-y-3">
                <input className="input w-full"placeholder="Ex.: Processo Trainee 2026"value={j}onChange={t=>k(t.target.value)}/>
                <textarea className="input min-h-24 w-full resize-y"placeholder="Descrição opcional"value={q}onChange={t=>D(t.target.value)}/>
                <N type="button"className="w-full gap-2"disabled={!j.trim()||z.isPending}onClick={Z}>
                  <Ne className="h-4 w-4"/>
                  Criar rascunho
                </N>
              </div>
            </section>

            <section className="overflow-hidden rounded-xl border border-border bg-card">
              <div className="border-b border-border p-4">
                <h2 className="text-sm font-semibold">Jornadas</h2>
              </div>
              {L.isLoading?<div className="p-4">
                  <Se rows={4}/>
                </div>:I.length===0?<div className="p-4 text-sm text-muted-foreground">Nenhuma jornada criada.</div>:<div className="divide-y divide-border">
                  {I.map(t=><button key={t.id}type="button"onClick={()=>x(t.id)}className={we("block w-full px-4 py-3 text-left hover:bg-accent",s===t.id&&"bg-accent")}>
                      <div className="flex items-start justify-between gap-3">
                        <div className="min-w-0">
                          <div className="truncate text-sm font-medium">{t.name}</div>
                          <div className="mt-1 text-xs text-muted-foreground">
                            {t.stepCount} avaliação(ões) · {t.sequenceCount} sequência(s)
                          </div>
                        </div>
                        <Je status={t.status}/>
                      </div>
                    </button>)}
                </div>}
            </section>
          </aside>

          <section className="space-y-5">
            {s?R.isLoading?<div className="rounded-xl border border-border bg-card p-5">
                <Se rows={6}/>
              </div>:R.isError||!C?<Ae tone="danger"title="Não foi possível carregar a jornada">
                {R.error instanceof Error?R.error.message:"Tente novamente."}
              </Ae>:<>
                <Na journey={C}simulations={H}selectedSimulationId={T}required={V}sequenceKey={O}onSimulationChange={$}onRequiredChange={W}onSequenceKeyChange={X}onAddStep={a}addPending={M.isPending}onPublish={()=>J({action:"publish",journeyId:C.id,journeyName:C.name})}publishPending={P.isPending}onArchive={()=>J({action:"archive",journeyId:C.id,journeyName:C.name})}archivePending={A.isPending}onRemoveStep={t=>J({action:"remove-step",journeyName:C.name,stepId:t.id,simulationName:t.simulationName})}onMoveStep={(t,ee)=>E.mutate({step:t,direction:ee})}/>
                <Aa published={C.status==="published"}/>
              </>:<Ce title="Selecione uma jornada"description="Crie ou escolha uma jornada para montar sua sequência de avaliações."/>}
          </section>
        </div>
      </main>

      <Ca confirmation={n}pending={G}error={Y}onCancel={()=>J(null)}onConfirm={S}/>
    </Oe>}i(la,"AssessmentJourneysPage");function Na({journey:e,simulations:s,selectedSimulationId:x,required:j,sequenceKey:k,onSimulationChange:q,onRequiredChange:D,onSequenceKeyChange:T,onAddStep:$,addPending:V,onPublish:W,publishPending:O,onArchive:X,archivePending:n,onRemoveStep:J,onMoveStep:L}){const R=se(),[F,I]=w(!1),[C,H]=w(e.name),[z,M]=w(e.description??"");Qe(()=>{I(!1),H(e.name),M(e.description??"")},[e.id,e.name,e.description]);const P=K({mutationFn:i(()=>oa(e.id,{name:C.trim(),description:z.trim()||null}),"mutationFn"),onSuccess:i(async a=>{I(!1),await Q(R,a.id)},"onSuccess")}),A=e.sequences,B=A.reduce((a,S)=>a+S.steps.length,0),E=e.status!=="archived",_=e.status==="draft"&&B>0,G=k.trim()||te,Y=new Set(A.find(a=>a.sequenceKey===G)?.steps.map(a=>a.simulationId)??[]),Z=s.filter(a=>!Y.has(a.id));return<section className="rounded-xl border border-border bg-card">
      <header className="flex flex-col gap-3 border-b border-border p-5 lg:flex-row lg:items-start lg:justify-between">
        <div>
          <div className="flex flex-wrap items-center gap-2">
            <Fe className="h-4 w-4 text-primary"/>
            <h2 className="text-xl font-semibold">{e.name}</h2>
            <Je status={e.status}/>
          </div>
          {e.description&&<p className="mt-1 text-sm text-muted-foreground">{e.description}</p>}
          <p className="mt-2 text-xs text-muted-foreground">
            {B} avaliação(ões) em {A.length} sequência(s)
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          {E&&<N variant="outline"size="sm"onClick={()=>I(a=>!a)}>
              <Be className="mr-1.5 h-4 w-4"/>
              {F?"Fechar edi\xE7\xE3o":"Editar"}
            </N>}
          <N variant="outline"size="sm"disabled={!_||O}onClick={W}>
            <Re className="mr-1.5 h-4 w-4"/>
            Publicar
          </N>
          <N variant="outline"size="sm"disabled={!E||n}onClick={X}>
            <De className="mr-1.5 h-4 w-4"/>
            Arquivar
          </N>
        </div>
      </header>

      {F&&E&&<div className="border-b border-border bg-muted/20 p-5">
          <div className="grid gap-3 lg:grid-cols-[1fr_1fr_auto]">
            <input className="input"value={C}onChange={a=>H(a.target.value)}/>
            <textarea className="input min-h-10"value={z}onChange={a=>M(a.target.value)}/>
            <div className="flex gap-2">
              <N disabled={!C.trim()||P.isPending}onClick={()=>P.mutate()}>
                <Ie className="mr-1.5 h-4 w-4"/>
                Salvar
              </N>
              <N variant="outline"disabled={P.isPending}onClick={()=>I(!1)}>
                <Me className="mr-1.5 h-4 w-4"/>
                Cancelar
              </N>
            </div>
          </div>
        </div>}

      {E&&<div className="border-b border-border p-5">
          {s.length===0?<Ce title="Nenhuma avaliação publicada"description="Publique uma avaliação antes de adicioná-la à jornada."actions={<N asChild>
                  <U to="/avaliacoes">Abrir avaliações</U>
                </N>}/>:<div className="grid gap-3 lg:grid-cols-[1fr_220px_150px_130px]">
              <select className="input"value={x}onChange={a=>q(a.target.value)}>
                <option value="">Selecione uma avaliação</option>
                {Z.map(a=><option key={a.id}value={a.id}>
                    {a.name} · v
                    {a.livePublishedVersionNumber??a.versionNumber}
                  </option>)}
              </select>
              <input className="input"list="journey-sequences"value={k}onChange={a=>T(a.target.value)}placeholder="Sequência"/>
              <datalist id="journey-sequences">
                {A.map(a=><option key={a.sequenceKey}value={a.sequenceKey}/>)}
              </datalist>
              <label className="inline-flex items-center gap-2 rounded-md border border-border px-3 text-sm">
                <input type="checkbox"checked={j}onChange={a=>D(a.target.checked)}/>
                Obrigatória
              </label>
              <N disabled={!x||!k.trim()||V}onClick={$}>
                <Ne className="mr-1.5 h-4 w-4"/>
                Adicionar
              </N>
            </div>}
        </div>}

      {B===0?<div className="p-5 text-sm text-muted-foreground">
          Adicione pelo menos uma avaliação para publicar a jornada.
        </div>:<div className="divide-y divide-border">
          {A.map(a=><div key={a.sequenceKey}className="p-5">
              <div className="mb-3 flex items-center gap-2">
                <span className="rounded-full border border-primary/30 bg-primary/10 px-2.5 py-0.5 text-xs font-medium text-primary">
                  {a.sequenceKey}
                </span>
                <span className="text-xs text-muted-foreground">
                  {a.steps.length} etapa(s)
                </span>
              </div>
              <div className="overflow-x-auto">
                <table className="w-full min-w-[680px] text-left text-sm">
                  <thead className="border-b border-border bg-muted/45 text-xs uppercase text-muted-foreground">
                    <tr>
                      <th className="px-4 py-3">Ordem</th>
                      <th className="px-4 py-3">Avaliação</th>
                      <th className="px-4 py-3">Versão</th>
                      <th className="px-4 py-3">Obrigatória</th>
                      <th className="px-4 py-3 text-right">Ações</th>
                    </tr>
                  </thead>
                  <tbody>
                    {a.steps.map((S,t)=><tr key={S.id}className="border-b border-border last:border-0">
                        <td className="px-4 py-3">{t+1}</td>
                        <td className="px-4 py-3 font-medium">{S.simulationName}</td>
                        <td className="px-4 py-3">v{S.simulationVersionNumber}</td>
                        <td className="px-4 py-3">{S.required?"Sim":"N\xE3o"}</td>
                        <td className="px-4 py-3 text-right">
                          {E&&<div className="flex justify-end gap-2">
                              <N variant="outline"size="sm"disabled={t===0}onClick={()=>L(S,-1)}>
                                Subir
                              </N>
                              <N variant="outline"size="sm"disabled={t===a.steps.length-1}onClick={()=>L(S,1)}>
                                Descer
                              </N>
                              <N variant="outline"size="sm"className="text-danger"aria-label={`Remover ${S.simulationName} da jornada`}onClick={()=>J(S)}>
                                <Le className="h-3.5 w-3.5"/>
                              </N>
                            </div>}
                        </td>
                      </tr>)}
                  </tbody>
                </table>
              </div>
            </div>)}
        </div>}
    </section>}i(Na,"JourneyComposer");function Ca({confirmation:e,pending:s,error:x,onCancel:j,onConfirm:k}){if(!e)return null;const q=Sa(e);return<_e open onOpenChange={D=>{!D&&!s&&j()}}>
      <He>
        <Xe>
          <Ge>{q.title}</Ge>
          <Ue>{q.description}</Ue>
          {x&&<p className="text-sm text-danger">
              {x instanceof Error?x.message:"N\xE3o foi poss\xEDvel concluir a a\xE7\xE3o."}
            </p>}
        </Xe>
        <We>
          <Ve disabled={s}>Cancelar</Ve>
          <$e disabled={s}onClick={D=>{D.preventDefault(),k()}}>
            {s?q.pendingLabel:q.actionLabel}
          </$e>
        </We>
      </He>
    </_e>}i(Ca,"JourneyConfirmationDialog");function Sa(e){return e.action==="publish"?{title:"Publicar jornada?",description:`A jornada ${e.journeyName} ficar\xE1 dispon\xEDvel para cria\xE7\xE3o de convites. Confirme que a composi\xE7\xE3o e a ordem das avalia\xE7\xF5es est\xE3o corretas.`,actionLabel:"Publicar jornada",pendingLabel:"Publicando..."}:e.action==="archive"?{title:"Arquivar jornada?",description:`A jornada ${e.journeyName} deixar\xE1 de aceitar altera\xE7\xF5es e novos convites. O hist\xF3rico e as participa\xE7\xF5es existentes ser\xE3o preservados.`,actionLabel:"Arquivar jornada",pendingLabel:"Arquivando..."}:{title:"Remover avalia\xE7\xE3o da jornada?",description:`A avalia\xE7\xE3o ${e.simulationName} ser\xE1 retirada da composi\xE7\xE3o de ${e.journeyName}. A avalia\xE7\xE3o original e os hist\xF3ricos j\xE1 registrados n\xE3o ser\xE3o exclu\xEDdos.`,actionLabel:"Remover avalia\xE7\xE3o",pendingLabel:"Removendo..."}}i(Sa,"confirmationCopy");function Aa({published:e}){return<section className="rounded-xl border border-border bg-card p-5">
      <h2 className="text-lg font-semibold">Convites e acompanhamento</h2>
      <p className="mt-1 text-sm text-muted-foreground">
        Esta jornada é configurada aqui. Pessoas participantes, links, validade, reenvios e
        resultados são administrados na Central de Participações.
      </p>
      <div className="mt-4 flex flex-wrap gap-2">
        <N asChild disabled={!e}>
          <U to="/participacoes/jornada">Criar convite por jornada</U>
        </N>
        <N asChild variant="outline">
          <U to="/participacoes">Abrir participações</U>
        </N>
      </div>
      {!e&&<p className="mt-3 text-xs text-muted-foreground">
          Publique a jornada para liberar a criação de convites.
        </p>}
    </section>}i(Aa,"ParticipationOwnerCard");function Je({status:e}){const s={draft:"Rascunho",published:"Publicada",archived:"Arquivada"},x=e==="published"?"border-success/30 bg-success/10 text-success":e==="archived"?"border-border bg-muted text-muted-foreground":"border-warning/30 bg-warning/10 text-warning-foreground";return<span className={we("rounded-full border px-2 py-0.5 text-[11px]",x)}>
      {s[e]}
    </span>}i(Je,"JourneyStatusBadge");async function Q(e,s){await e.invalidateQueries({queryKey:["assessment-journeys"]}),s&&await e.invalidateQueries({queryKey:["assessment-journey",s]})}i(Q,"invalidateJourney");export{Ka as Route};