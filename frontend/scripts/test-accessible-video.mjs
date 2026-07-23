import fs from 'node:fs';

const api = fs.readFileSync(new URL('../src/lib/api/praxis-legacy.ts', import.meta.url), 'utf8');
const candidate = fs.readFileSync(new URL('../src/features/candidate/candidate-experience.tsx', import.meta.url), 'utf8');
const editor = fs.readFileSync(new URL('../src/routes/nova.dialogo.tsx', import.meta.url), 'utf8');
const integrity = fs.readFileSync(new URL('../src/components/candidate-integrity-boundary.tsx', import.meta.url), 'utf8');
const dashboard = fs.readFileSync(new URL('../src/routes/dashboard.tsx', import.meta.url), 'utf8');

for (const token of ['"VIDEO"', 'mediaTranscript', 'mediaCaptionsUrl', 'mediaVersion']) {
  if (!api.includes(token)) throw new Error(`Contrato ausente: ${token}`);
}
for (const token of ['<video', 'kind="captions"', 'Velocidade de reprodução', 'Transcrição', 'preload="metadata"']) {
  if (!candidate.includes(token)) throw new Error(`Experiência do candidato sem ${token}`);
}
for (const token of ['video/mp4', 'URL da legenda WebVTT', 'Transcrição acessível', 'Versão da mídia']) {
  if (!editor.includes(token)) throw new Error(`Editor sem ${token}`);
}
for (const token of ['MAX_VIDEO_DURATION_SECONDS', 'O vídeo deve ter no máximo 10 minutos']) {
  if (!editor.includes(token)) throw new Error(`Validação de duração ausente: ${token}`);
}
for (const token of ['VIDEO_PLAYBACK_STARTED', 'VIDEO_PLAYBACK_PAUSED', 'VIDEO_PLAYBACK_COMPLETED', 'VIDEO_PLAYBACK_ERROR']) {
  if (!integrity.includes(token)) throw new Error(`Telemetria de vídeo ausente: ${token}`);
}
if (!dashboard.includes('mediaQualityComparisons')) throw new Error('Comparação de qualidade por mídia ausente.');
if (candidate.includes('autoPlay')) throw new Error('Vídeo não pode iniciar automaticamente.');
console.log('Contrato de vídeo acessível validado.');
