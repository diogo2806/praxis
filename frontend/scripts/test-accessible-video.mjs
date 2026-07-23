import fs from 'node:fs';

const api = fs.readFileSync(new URL('../src/lib/api/praxis-legacy.ts', import.meta.url), 'utf8');
const candidate = fs.readFileSync(new URL('../src/features/candidate/candidate-experience.tsx', import.meta.url), 'utf8');
const editor = fs.readFileSync(new URL('../src/routes/nova.dialogo.tsx', import.meta.url), 'utf8');

for (const token of ['"VIDEO"', 'mediaTranscript', 'mediaCaptionsUrl', 'mediaVersion']) {
  if (!api.includes(token)) throw new Error(`Contrato ausente: ${token}`);
}
for (const token of ['<video', 'kind="captions"', 'Velocidade de reprodução', 'Transcrição', 'preload="metadata"']) {
  if (!candidate.includes(token)) throw new Error(`Experiência do candidato sem ${token}`);
}
for (const token of ['video/mp4', 'URL da legenda WebVTT', 'Transcrição acessível', 'Versão da mídia']) {
  if (!editor.includes(token)) throw new Error(`Editor sem ${token}`);
}
if (candidate.includes('autoPlay')) throw new Error('Vídeo não pode iniciar automaticamente.');
console.log('Contrato de vídeo acessível validado.');
