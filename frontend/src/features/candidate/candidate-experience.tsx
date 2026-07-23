import { useMutation, useQuery } from "@tanstack/react-query";
import { useCallback, useEffect, useMemo, useRef, useState, type CSSProperties } from "react";
import {
  getCandidateAttempt,
  HEALTH_CONSENT_VERSION,
  PraxisApiError,
  recordHealthConsent,
  requestHumanReview,
  submitCandidateAnswer,
  type CandidateAttemptResponse,
  type CandidateNodeResponse,
  type MediaType,
} from "@/lib/api/praxis";
import { useLanguage } from "@/lib/language-context";
import { cn } from "@/lib/utils";
import "./candidate-experience.css";

const ACCESSIBILITY_STORAGE_KEY = "praxis:candidate-accessibility";
const OPTION_LETTERS = ["A", "B", "C", "D", "E", "F", "G", "H"];

type CandidateExecutionCopy = ReturnType<typeof useLanguage>["t"]["candidateExecution"];

type CandidateAccessibilityPreferences = {
  highContrast: boolean;
  largeText: boolean;
  dyslexiaFont: boolean;
};

const DEFAULT_ACCESSIBILITY_PREFERENCES: CandidateAccessibilityPreferences = {
  highContrast: false,
  largeText: false,
  dyslexiaFont: false,
};

function loadAccessibilityPreferences(): CandidateAccessibilityPreferences {
  if (typeof window === "undefined") {
    return DEFAULT_ACCESSIBILITY_PREFERENCES;
  }
  try {
    const stored = window.localStorage.getItem(ACCESSIBILITY_STORAGE_KEY);
    if (!stored) {
      return DEFAULT_ACCESSIBILITY_PREFERENCES;
    }
    const parsed = JSON.parse(stored) as Partial<CandidateAccessibilityPreferences>;
    return {
      highContrast: Boolean(parsed.highContrast),
      largeText: Boolean(parsed.largeText),
      dyslexiaFont: Boolean(parsed.dyslexiaFont),
    };
  } catch {
    return DEFAULT_ACCESSIBILITY_PREFERENCES;
  }
}

function friendlyApiErrorMessage(
  error: unknown,
  messagesByStatus: Record<number, string>,
  fallback: string,
) {
  if (error instanceof PraxisApiError) {
    return messagesByStatus[error.status] ?? fallback;
  }
  return fallback;
}

function CandidateMedia({
  mediaUrl,
  mediaType,
  accessibleDescription,
  transcript,
  captionsUrl,
  mediaVersion,
  audioLabel,
  unsupportedAudio,
}: {
  mediaUrl: string;
  mediaType: MediaType | null;
  accessibleDescription?: string | null;
  transcript?: string | null;
  captionsUrl?: string | null;
  mediaVersion?: string | null;
  audioLabel: string;
  unsupportedAudio: string;
}) {
  const [playbackRate, setPlaybackRate] = useState(1);
  const mediaRef = useRef<HTMLMediaElement>(null);

  useEffect(() => {
    if (mediaRef.current) mediaRef.current.playbackRate = playbackRate;
  }, [playbackRate]);

  if (mediaType === "AUDIO") {
    return (
      <div className="space-y-2">
        <audio ref={mediaRef as React.RefObject<HTMLAudioElement>} controls preload="metadata" src={mediaUrl} className="w-full" aria-label={audioLabel} onClick={(event) => event.stopPropagation()}>
          {unsupportedAudio}
        </audio>
        <PlaybackRateControl value={playbackRate} onChange={setPlaybackRate} />
        {transcript && <details><summary>Transcrição</summary><p className="mt-2 whitespace-pre-wrap">{transcript}</p></details>}
      </div>
    );
  }

  if (mediaType === "VIDEO") {
    return (
      <div className="space-y-2" data-media-version={mediaVersion ?? undefined}>
        <video ref={mediaRef as React.RefObject<HTMLVideoElement>} controls preload="metadata" playsInline className="max-h-[28rem] w-full rounded-md border border-border bg-black" aria-label={accessibleDescription?.trim() || "Vídeo do cenário"} onClick={(event) => event.stopPropagation()}>
          <source src={mediaUrl} />
          {captionsUrl && <track kind="captions" src={captionsUrl} srcLang="pt-BR" label="Português" default />}
          Seu navegador não suporta vídeo.
        </video>
        <PlaybackRateControl value={playbackRate} onChange={setPlaybackRate} />
        {transcript && <details><summary>Transcrição</summary><p className="mt-2 whitespace-pre-wrap">{transcript}</p></details>}
      </div>
    );
  }

  return <img src={mediaUrl} alt={accessibleDescription?.trim() || ""} className="max-h-48 w-auto rounded-md border border-border object-contain" data-media-version={mediaVersion ?? undefined} />;
}

function PlaybackRateControl({ value, onChange }: { value: number; onChange: (value: number) => void }) {
  return (
    <label className="inline-flex items-center gap-2 text-sm">
      Velocidade
      <select value={value} onChange={(event) => onChange(Number(event.target.value))} aria-label="Velocidade de reprodução">
        {[0.5, 0.75, 1, 1.25, 1.5, 2].map((rate) => <option key={rate} value={rate}>{rate}×</option>)}
      </select>
    </label>
  );
}

function formatTimer(seconds: number): string {
  const minutes = Math.floor(seconds / 60);
  const remainder = seconds % 60;
  return `${String(minutes).padStart(2, "0")}:${String(remainder).padStart(2, "0")}`;
}

function FocusedCandidateExperience({ token }: { token: string }) {
  const { t } = useLanguage();
  const copy = t.candidateExecution;
  const [liveAttempt, setLiveAttempt] = useState<CandidateAttemptResponse | null>(null);
  const [remaining, setRemaining] = useState(30);
  const [selectedOptionId, setSelectedOptionId] = useState<string | null>(null);
  const [accessibilityPreferences, setAccessibilityPreferences] = useState(
    loadAccessibilityPreferences,
  );
  const [submittingAnswer, setSubmittingAnswer] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [healthConsentGiven, setHealthConsentGiven] = useState(false);
  const { highContrast, largeText, dyslexiaFont } = accessibilityPreferences;

  const submitErrorMessages = useMemo<Record<number, string>>(
    () => ({
      400: copy.errors.submitBadRequest,
      409: copy.errors.submitConflict,
    }),
    [copy.errors.submitBadRequest, copy.errors.submitConflict],
  );
  const loadErrorMessages = useMemo<Record<number, string>>(
    () => ({
      400: copy.errors.loadBadRequest,
      404: copy.errors.loadNotFound,
      409: copy.errors.loadConflict,
    }),
    [copy.errors.loadBadRequest, copy.errors.loadConflict, copy.errors.loadNotFound],
  );

  const attemptQuery = useQuery({
    queryKey: ["candidate-attempt", token],
    queryFn: () => getCandidateAttempt(token),
  });

  useEffect(() => {
    if (attemptQuery.data) {
      setLiveAttempt(attemptQuery.data);
    }
  }, [attemptQuery.data]);

  useEffect(() => {
    window.localStorage.setItem(
      ACCESSIBILITY_STORAGE_KEY,
      JSON.stringify(accessibilityPreferences),
    );
  }, [accessibilityPreferences]);

  const attempt = liveAttempt ?? attemptQuery.data;
  const currentNode = attempt?.etapaAtual ?? null;
  const finished = Boolean(attempt && (attempt.finalizado || !currentNode));
  const needsHealthConsent =
    Boolean(attempt?.verticalSaude) &&
    !healthConsentGiven &&
    !attempt?.healthConsentValid &&
    !finished;
  const timeLimit = Math.max(
    1,
    currentNode?.tempoLimiteSegundosAcomodado ?? currentNode?.tempoLimiteSegundos ?? 30,
  );
  const timePercentage = finished ? 0 : Math.max(0, Math.min(100, (remaining / timeLimit) * 100));
  const currentStep = attempt?.progresso?.passoAtual ?? currentNode?.numero ?? 1;
  const totalSteps = Math.max(currentStep, attempt?.progresso?.passosEstimados ?? currentStep);
  const selectedOption = currentNode?.alternativas.find((option) => option.id === selectedOptionId);

  useEffect(() => {
    const redirectUrl = attempt?.redirectUrl;
    if (!finished || !redirectUrl) return;

    const timeout = window.setTimeout(() => {
      window.location.assign(redirectUrl);
    }, 1200);
    return () => window.clearTimeout(timeout);
  }, [attempt?.redirectUrl, finished]);

  const submitAnswer = useCallback(
    async (node: CandidateNodeResponse, optionId: string | null, timedOut: boolean) => {
      if (!attempt) return;
      setSubmittingAnswer(true);
      setSubmitError(null);

      const sendAnswer = async (timeExpired: boolean) => {
        const response = await submitCandidateAnswer(token, {
          etapaId: node.id,
          etapaNumero: node.numero,
          respostaId: timeExpired ? null : optionId,
          respondidaEm: new Date().toISOString(),
          tempoEsgotado: timeExpired,
        });
        setLiveAttempt({
          participacaoId: response.participacaoId,
          avaliacaoNome: attempt.avaliacaoNome,
          status: response.status,
          finalizado: response.finalizado,
          redirectUrl: response.redirectUrl ?? attempt.redirectUrl ?? null,
          acaoSugeridaFrontend: response.finalizado
            ? "VER_RESULTADOS"
            : attempt.acaoSugeridaFrontend,
          progresso: response.progresso,
          etapaAtual: response.etapaAtual,
          verticalSaude: attempt.verticalSaude,
          healthConsentValid: attempt.healthConsentValid,
          healthConsentNoticeVersion: attempt.healthConsentNoticeVersion,
        });
        setSelectedOptionId(null);
        void attemptQuery.refetch();
      };

      try {
        await sendAnswer(timedOut);
      } catch (error) {
        if (!timedOut && error instanceof PraxisApiError && error.status === 409) {
          try {
            await sendAnswer(true);
            return;
          } catch (retryError) {
            setSubmitError(
              friendlyApiErrorMessage(
                retryError,
                submitErrorMessages,
                copy.errors.submitFallback,
              ),
            );
            setSelectedOptionId(null);
            return;
          }
        }
        setSubmitError(
          friendlyApiErrorMessage(error, submitErrorMessages, copy.errors.submitFallback),
        );
        setSelectedOptionId(null);
      } finally {
        setSubmittingAnswer(false);
      }
    },
    [attempt, attemptQuery, copy.errors.submitFallback, submitErrorMessages, token],
  );

  useEffect(() => {
    setRemaining(timeLimit);
    setSelectedOptionId(null);
  }, [currentNode?.numero, timeLimit]);

  useEffect(() => {
    if (finished || submittingAnswer || !currentNode || needsHealthConsent) return;
    const id = window.setInterval(() => {
      setRemaining((value) => Math.max(0, value - 1));
    }, 1000);
    return () => window.clearInterval(id);
  }, [submittingAnswer, currentNode, finished, needsHealthConsent]);

  useEffect(() => {
    if (
      remaining === 0 &&
      !finished &&
      currentNode &&
      !selectedOptionId &&
      !submittingAnswer &&
      !needsHealthConsent
    ) {
      void submitAnswer(currentNode, null, true);
    }
  }, [
    currentNode,
    finished,
    remaining,
    selectedOptionId,
    submitAnswer,
    submittingAnswer,
    needsHealthConsent,
  ]);

  const rootClass = cn("cand-root", highContrast && "hc", largeText && "large-text");
  const fontStyle = {
    fontFamily: dyslexiaFont
      ? "'OpenDyslexic', 'Atkinson Hyperlegible', Verdana, Arial, sans-serif"
      : undefined,
  } satisfies CSSProperties;

  return (
    <main className={rootClass} style={fontStyle}>
      <div className="cand-a11y">
        <button
          type="button"
          onClick={() =>
            setAccessibilityPreferences((value) => ({
              ...value,
              highContrast: !value.highContrast,
            }))
          }
          aria-pressed={highContrast}
          title={copy.accessibility.highContrastTitle}
        >
          {copy.accessibility.highContrast}
        </button>
        <button
          type="button"
          onClick={() =>
            setAccessibilityPreferences((value) => ({
              ...value,
              largeText: !value.largeText,
            }))
          }
          aria-pressed={largeText}
          title={copy.accessibility.largerTextTitle}
        >
          {largeText ? copy.accessibility.normalText : copy.accessibility.largerText}
        </button>
        <button
          type="button"
          onClick={() =>
            setAccessibilityPreferences((value) => ({
              ...value,
              dyslexiaFont: !value.dyslexiaFont,
            }))
          }
          aria-pressed={dyslexiaFont}
          title={copy.accessibility.dyslexiaFontTitle}
        >
          {copy.accessibility.dyslexiaFont}
        </button>
      </div>

      {attemptQuery.isLoading ? (
        <div className="cand-status">
          <div className="cs-label loading">{copy.loading.label}</div>
          <h1>{copy.loading.title}</h1>
        </div>
      ) : attemptQuery.isError && !attempt ? (
        <div className="cand-status">
          <div className="cs-label error">{copy.accessError.label}</div>
          <h1>{copy.accessError.title}</h1>
          <p>
            {friendlyApiErrorMessage(
              attemptQuery.error,
              loadErrorMessages,
              copy.errors.loadFallback,
            )}
          </p>
        </div>
      ) : needsHealthConsent ? (
        <HealthConsentGate
          token={token}
          copy={copy}
          noticeVersion={attempt?.healthConsentNoticeVersion ?? HEALTH_CONSENT_VERSION}
          onConsented={async () => {
            const refreshedAttempt = await attemptQuery.refetch();
            if (refreshedAttempt.data?.healthConsentValid) {
              setLiveAttempt(refreshedAttempt.data);
              setHealthConsentGiven(true);
            }
          }}
        />
      ) : currentNode && !finished ? (
        <>
          <div className="cand-progress">
            <div className="cp-info">
              <span>{copy.scenario.progress(currentStep, totalSteps)}</span>
              <span>{remaining}s</span>
            </div>
            <div className="cp-track">
              <div
                className={cn("cp-fill", timePercentage <= 35 && "low")}
                style={{ width: `${timePercentage}%` }}
              />
            </div>
          </div>

          <div className="scenario" aria-label={copy.scenario.ariaLabel}>
            <div className="sc-top">
              <div className="sc-id">
                <div className="avatar">
                  {(currentNode.pessoa ?? "P").substring(0, 2).toUpperCase()}
                </div>
                <div>
                  <div className="who">{currentNode.pessoa ?? copy.scenario.participant}</div>
                  <div className="stage">
                    {copy.scenario.progress(currentStep, totalSteps)} · {attempt?.avaliacaoNome ?? copy.scenario.assessment}
                  </div>
                </div>
              </div>
              <div className="sc-timer">
                <span className="tdot" />
                {formatTimer(remaining)}
              </div>
            </div>

            <div className="sc-body">
              {currentNode.pessoa && <div className="sc-tag">{currentNode.pessoa}</div>}
              <p
                className="sc-msg"
                aria-label={currentNode.descricaoAcessivel || currentNode.descricao}
              >
                {currentNode.descricao}
              </p>

              {currentNode.audioDescricaoUrl && (
                <div className="sc-media">
                  <audio
                    controls
                    src={currentNode.audioDescricaoUrl}
                    aria-label={copy.media.scenarioAudioDescription}
                  >
                    {copy.media.unsupportedAudio}
                  </audio>
                </div>
              )}
              {currentNode.midiaUrl && (
                <div className="sc-media">
                  <CandidateMedia
                    mediaUrl={currentNode.midiaUrl}
                    mediaType={currentNode.tipoMidia ?? null}
                    accessibleDescription={currentNode.descricaoAcessivel}
                    transcript={currentNode.transcricaoMidia}
                    captionsUrl={currentNode.legendaMidiaUrl}
                    mediaVersion={currentNode.versaoMidia}
                    audioLabel={copy.media.scenarioAudio}
                    unsupportedAudio={copy.media.unsupportedAudio}
                  />
                </div>
              )}

              <div className="sc-opts" role="group" aria-label={copy.scenario.question}>
                {currentNode.alternativas.map((option, idx) => {
                  const optionLabel = OPTION_LETTERS[idx] ?? idx + 1;
                  return (
                    <div key={option.id}>
                      <button
                        type="button"
                        className={cn("opt", selectedOptionId === option.id && "picked")}
                        onClick={() => {
                          setSubmitError(null);
                          setSelectedOptionId(option.id);
                        }}
                        disabled={submittingAnswer}
                        aria-label={option.descricaoAcessivel || option.texto}
                        aria-pressed={selectedOptionId === option.id}
                      >
                        <span className="key">{optionLabel}</span>
                        <span>{option.texto}</span>
                      </button>
                      {option.audioDescricaoUrl && (
                        <div className="sc-media">
                          <audio
                            controls
                            src={option.audioDescricaoUrl}
                            aria-label={copy.media.optionAudioDescription(optionLabel)}
                          >
                            {copy.media.unsupportedAudio}
                          </audio>
                        </div>
                      )}
                      {option.mediaUrl && (
                        <div className="sc-media">
                          <CandidateMedia
                            mediaUrl={option.mediaUrl}
                            mediaType={option.tipoMidia ?? null}
                            accessibleDescription={option.descricaoAcessivel}
                            transcript={option.transcricaoMidia}
                            captionsUrl={option.legendaMidiaUrl}
                            mediaVersion={option.versaoMidia}
                            audioLabel={copy.media.optionAudio(optionLabel)}
                            unsupportedAudio={copy.media.unsupportedAudio}
                          />
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>

              {selectedOption && (
                <div className="sc-confirm" aria-live="polite">
                  <span className="confirm-hint">{copy.scenario.confirmHint}</span>
                  <button
                    type="button"
                    onClick={() => submitAnswer(currentNode, selectedOption.id, false)}
                    disabled={submittingAnswer}
                  >
                    {submittingAnswer ? copy.scenario.confirming : copy.scenario.confirm}
                  </button>
                </div>
              )}
              {submitError && (
                <div className="sc-error" aria-live="assertive">
                  {submitError}
                </div>
              )}

              <p className="sc-note">
                <svg
                  viewBox="0 0 24 24"
                  fill="none"
                  strokeWidth="2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  aria-hidden="true"
                >
                  <path d="M12 2 4 5v6c0 5 3.4 8.3 8 11 4.6-2.7 8-6 8-11V5z" />
                </svg>
                <span>{copy.scenario.note}</span>
              </p>
            </div>
          </div>
        </>
      ) : (
        <div className="cand-status">
          <div className="cs-label done">{copy.completion.label}</div>
          <h1>{copy.completion.title}</h1>
          <p>{attempt?.redirectUrl ? copy.completion.redirecting : copy.completion.processing}</p>
          <div className="cs-note">{copy.completion.explanation}</div>
          <HumanReviewRequest attemptId={token} copy={copy.review} />
        </div>
      )}

      <div className="cand-footer">
        {attempt?.verticalSaude ? copy.footer.health : copy.footer.standard}
      </div>
    </main>
  );
}

function HumanReviewRequest({
  attemptId,
  copy,
}: {
  attemptId: string;
  copy: CandidateExecutionCopy["review"];
}) {
  const [open, setOpen] = useState(false);
  const [reason, setReason] = useState("");
  const mutation = useMutation({
    mutationFn: () => requestHumanReview(attemptId, reason),
  });

  if (mutation.isSuccess) {
    return (
      <div className="cand-review" aria-live="polite">
        <p className="rev-ok">{copy.success}</p>
      </div>
    );
  }

  return (
    <div className="cand-review">
      {!open ? (
        <button type="button" className="link-btn" onClick={() => setOpen(true)}>
          {copy.request}
        </button>
      ) : (
        <div>
          <textarea
            value={reason}
            onChange={(event) => setReason(event.target.value)}
            rows={3}
            maxLength={1000}
            placeholder={copy.placeholder}
          />
          <button
            type="button"
            className="rev-btn"
            disabled={mutation.isPending}
            onClick={() => mutation.mutate()}
          >
            {mutation.isPending ? copy.sending : copy.send}
          </button>
          {mutation.isError && (
            <p className="rev-err" aria-live="assertive">
              {copy.error}
            </p>
          )}
        </div>
      )}
    </div>
  );
}

function HealthConsentGate({
  token,
  copy,
  noticeVersion,
  onConsented,
}: {
  token: string;
  copy: CandidateExecutionCopy;
  noticeVersion: string;
  onConsented: () => Promise<void> | void;
}) {
  const [agreed, setAgreed] = useState(false);
  const [onBehalfOfMinor, setOnBehalfOfMinor] = useState(false);
  const mutation = useMutation({
    mutationFn: () => recordHealthConsent(token, onBehalfOfMinor, noticeVersion),
    onSuccess: () => onConsented(),
  });
  const healthCopy = copy.healthConsent;

  return (
    <div className="cand-consent">
      <div className="cc-label">{healthCopy.beforeStart}</div>
      <h1>{healthCopy.title}</h1>
      <div className="cc-body">
        <p>
          {healthCopy.educationalPrefix}
          <strong>{healthCopy.educationalStrong}</strong>
          {healthCopy.educationalSuffix}
        </p>
        <p>
          <strong>{copy.footer.health}</strong>
        </p>
        <p>
          {healthCopy.purposePrefix}
          <strong>{healthCopy.purposeStrong}</strong>
          {healthCopy.purposeSuffix}
        </p>
        <ul>
          {healthCopy.bullets.map((item) => (
            <li key={item}>{item}</li>
          ))}
        </ul>
      </div>

      <div className="cc-checks">
        <label className="cc-check">
          <input
            type="checkbox"
            checked={agreed}
            onChange={(event) => setAgreed(event.target.checked)}
          />
          <span>{healthCopy.consent}</span>
        </label>
        <label className="cc-check">
          <input
            type="checkbox"
            checked={onBehalfOfMinor}
            onChange={(event) => setOnBehalfOfMinor(event.target.checked)}
          />
          <span>{healthCopy.guardian}</span>
        </label>
      </div>

      <div className="cc-actions">
        <button
          type="button"
          className="cc-btn"
          disabled={!agreed || mutation.isPending}
          onClick={() => mutation.mutate()}
        >
          {mutation.isPending ? healthCopy.registering : healthCopy.continue}
        </button>
        {mutation.isError && (
          <p className="cc-err" aria-live="assertive">
            {healthCopy.error}
          </p>
        )}
      </div>
    </div>
  );
}

export function CandidateExperience({ token }: { token: string }) {
  return <FocusedCandidateExperience token={token} />;
}
