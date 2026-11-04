// Gera a coleção e o environment do Postman em Docs/postman. Uso: node scripts/gerar-colecao-postman.js
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

const OUT = path.join(__dirname, '..', 'Docs', 'postman');
const TOKENS = { admin: 'adminToken', escola: 'escolaToken', motorista: 'motoristaToken', respA: 'respAToken', respB: 'respBToken' };

const folders = [];
let cur = null;
let folderNo = -1;

function folder(name, description) {
  folderNo += 1;
  cur = { n: folderNo, name: folderNo + '. ' + name, description, seq: 0, items: [] };
  folders.push(cur);
}

// m: método, p: caminho, who: persona (omitido = sem token), ok: status aceitos, save: {variavel: expressao em json}
function r(o) {
  cur.seq += 1;
  cur.items.push(Object.assign({ num: cur.n + '.' + cur.seq }, o));
}

const lines = (arr) => arr.join('\n');
const j = (o) => JSON.stringify(o, null, 2);

// ---------- Scripts ----------
const SETUP = lines([
  "var rnd = function (n) { return Math.floor(Math.random() * n); };",
  "var digits = function (n) { var a = []; for (var i = 0; i < n; i++) { a.push(rnd(10)); } return a; };",
  "var dv = function (a, w) { var s = 0; for (var i = 0; i < a.length; i++) { s += a[i] * w[i]; } var r = s % 11; return r < 2 ? 0 : 11 - r; };",
  "var cpf = function () {",
  "  var n; do { n = digits(9); } while (n.every(function (x) { return x === n[0]; }));",
  "  var d1 = dv(n, [10, 9, 8, 7, 6, 5, 4, 3, 2]);",
  "  var d2 = dv(n.concat([d1]), [11, 10, 9, 8, 7, 6, 5, 4, 3, 2]);",
  "  var t = n.concat([d1, d2]).join('');",
  "  return t.slice(0, 3) + '.' + t.slice(3, 6) + '.' + t.slice(6, 9) + '-' + t.slice(9);",
  "};",
  "var cnpj = function () {",
  "  var n = digits(8).concat([0, 0, 0, 1]);",
  "  var d1 = dv(n, [5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2]);",
  "  var d2 = dv(n.concat([d1]), [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2]);",
  "  var t = n.concat([d1, d2]).join('');",
  "  return t.slice(0, 2) + '.' + t.slice(2, 5) + '.' + t.slice(5, 8) + '/' + t.slice(8, 12) + '-' + t.slice(12);",
  "};",
  "var L = 'ABCDEFGHJKLMNPRSTUVWXYZ';",
  "var placa = L[rnd(L.length)] + L[rnd(L.length)] + L[rnd(L.length)] + rnd(10) + (rnd(2) ? String(rnd(10)) : L[rnd(L.length)]) + rnd(10) + rnd(10);",
  "var fmt = function (d) { return d.getFullYear() + '-' + ('0' + (d.getMonth() + 1)).slice(-2) + '-' + ('0' + d.getDate()).slice(-2); };",
  "var addDays = function (n) { var d = new Date(); d.setDate(d.getDate() + n); return fmt(d); };",
  "var runId = Date.now().toString(36);",
  "var vars = {",
  "  runId: runId,",
  "  emailEscola: 'escola.' + runId + '@tiodaperua.test',",
  "  emailMotorista: 'motorista.' + runId + '@tiodaperua.test',",
  "  emailRespA: 'respa.' + runId + '@tiodaperua.test',",
  "  emailRespB: 'respb.' + runId + '@tiodaperua.test',",
  "  emailExtra: 'extra.' + runId + '@tiodaperua.test',",
  "  cpfMotorista: cpf(), cpfRespA: cpf(), cpfRespB: cpf(), cpfExtra: cpf(), cpfDep1: cpf(), cpfDep2: cpf(),",
  "  cnpjEscola: cnpj(),",
  "  placaVeiculo: placa,",
  "  cnhNumero: digits(11).join(''),",
  "  renavam: digits(11).join(''),",
  "  dataHoje: addDays(0), dataAmanha: addDays(1),",
  "  dataValidade: addDays(365), dataRealizacao: addDays(-30)",
  "};",
  "Object.keys(vars).forEach(function (k) { pm.environment.set(k, vars[k]); });",
  "console.log('Dados desta execucao gerados. runId=' + runId);"
]);

const REORDENAR = lines([
  "var ids = JSON.parse(pm.environment.get('paradasIds'));",
  "var meio = ids.slice(1, -1);",
  "if (meio.length >= 2) { var t = meio[0]; meio[0] = meio[1]; meio[1] = t; }",
  "pm.environment.set('idsParadasReordenadas', JSON.stringify([ids[0]].concat(meio, [ids[ids.length - 1]])));"
]);

const SYNC_PRE = lines([
  "pm.environment.set('eventoUuid1', pm.variables.replaceIn('{{$guid}}'));",
  "pm.environment.set('eventoUuid2', pm.variables.replaceIn('{{$guid}}'));",
  "pm.environment.set('ocorridoEm1', new Date(Date.now() - 20000).toISOString());",
  "pm.environment.set('ocorridoEm2', new Date(Date.now() - 10000).toISOString());"
]);

// ---------- Geração do JSON ----------
const allScripts = [];

function script(type, code) {
  allScripts.push(code);
  return { listen: type, script: { type: 'text/javascript', exec: code.split('\n') } };
}

function buildTest(o) {
  const ok = o.ok || [200];
  const out = [];
  out.push('pm.test("Status ' + ok.join(' ou ') + '", function () { pm.expect(' + JSON.stringify(ok) + ').to.include(pm.response.code); });');
  out.push('var json = null; try { json = pm.response.json(); } catch (e) { json = null; }');
  out.push('var sucesso = ' + JSON.stringify(ok) + '.indexOf(pm.response.code) >= 0;');
  Object.keys(o.save || {}).forEach((k) => {
    out.push('if (sucesso && json) { try { var v = (' + o.save[k] + '); if (v !== undefined && v !== null) { pm.environment.set("' + k + '", String(v)); } } catch (e) {} }');
  });
  (o.checks || []).forEach((c) => {
    out.push('pm.test(' + JSON.stringify(c[0]) + ', function () { pm.expect(Boolean(' + c[1] + ')).to.be.true; });');
  });
  if (o.test) out.push(o.test);
  return out.join('\n');
}

function buildItem(o) {
  const name = o.num + ' ' + o.m + ' ' + (o.p || '') + (o.label ? ' - ' + o.label : '');
  const hasBody = o.body !== undefined;
  const raw = hasBody ? (typeof o.body === 'string' ? o.body : j(o.body)) : undefined;
  const query = Object.keys(o.q || {}).map((k) => ({ key: k, value: o.q[k] }));
  const qs = query.length ? '?' + query.map((x) => x.key + '=' + x.value).join('&') : '';

  let url;
  if (o.abs) {
    const u = new URL(o.abs);
    url = { raw: o.abs, protocol: u.protocol.replace(':', ''), host: u.hostname.split('.'), path: u.pathname.split('/').filter(Boolean) };
  } else {
    url = { raw: '{{baseUrl}}' + o.p + qs, host: ['{{baseUrl}}'], path: o.p.split('/').filter(Boolean) };
    if (query.length) url.query = query;
  }

  const header = [{ key: 'Accept', value: 'application/json' }];
  if (hasBody) header.push({ key: 'Content-Type', value: 'application/json' });

  const auth = o.who
    ? { type: 'bearer', bearer: [{ key: 'token', value: '{{' + TOKENS[o.who] + '}}', type: 'string' }] }
    : { type: 'noauth' };

  const event = [];
  if (o.pre) event.push(script('prerequest', o.pre));
  event.push(script('test', buildTest(o)));

  const request = { method: o.m, header, url, auth };
  if (hasBody) request.body = { mode: 'raw', raw, options: { raw: { language: 'json' } } };
  if (o.d) request.description = o.d;
  return { name, event, request };
}

// ---------- Blocos reutilizáveis ----------
const usuario = (emailVar, tipo) => ({
  email: '{{' + emailVar + '}}',
  senha: '{{senhaPadrao}}',
  endereco: 'Avenida Paulista, 1000 - Bela Vista, São Paulo/SP',
  telefone: '(11) 98765-4321',
  tipoPerfil: tipo,
  aceitouTermos: true
});

const endereco = (cep, logradouro, numero) => ({
  cep, logradouro, numero, complemento: 'Sala 1', bairro: 'Bela Vista', cidade: 'São Paulo', uf: 'SP'
});

const motoristaBody = (emailVar, cpfVar) => ({
  nome: 'João Motorista Teste',
  dataNascimento: '1985-05-20',
  cpf: '{{' + cpfVar + '}}',
  usuarioDTO: usuario(emailVar, 'MOTORISTA'),
  enderecoOperacionalDTO: endereco('01310-100', 'Avenida Paulista', '1000')
});

const respBody = (nome, emailVar, cpfVar) => ({
  nome,
  cpf: '{{' + cpfVar + '}}',
  dataNascimento: '1990-10-15',
  usuario: usuario(emailVar, 'RESPONSAVEL')
});

const login = (who, emailVar, pwdVar, label, extra) =>
  r(Object.assign({
    m: 'POST', p: '/Autenticacao/Entrar', label,
    body: { email: '{{' + emailVar + '}}', senha: '{{' + pwdVar + '}}' },
    ok: [200],
    save: Object.assign({ [who + 'Token']: 'json.accessToken', [who + 'Refresh']: 'json.refreshToken' }, extra || {}),
    checks: [['Retorna accessToken e refreshToken', 'json && json.accessToken && json.refreshToken']]
  }));

const enderecoDep = (numero, finalidade) => ({
  cep: '01310-100', logradouro: 'Avenida Paulista', numero, complemento: 'Apto 12', bairro: 'Bela Vista',
  cidade: 'São Paulo', uf: 'SP', finalidade
});

// =====================================================================
folder('Preparação', 'Gera os dados únicos da execução (CPF/CNPJ válidos, e-mails, placa, datas) e confere a conectividade. Rode sempre primeiro.');
r({
  m: 'GET', p: '/v3/api-docs', label: 'health check + geração de dados',
  ok: [200], pre: SETUP,
  checks: [['OpenAPI disponível (DOCS_ENABLED=true)', 'json && json.openapi']],
  d: 'Confirma que o backend está no ar e gera todos os dados únicos da execução nas variáveis do environment.'
});
r({
  m: 'GET', abs: 'https://viacep.com.br/ws/01310100/json/', label: 'ViaCEP (conectividade)', ok: [200],
  checks: [['CEP 01310-100 encontrado', 'json && json.erro !== true']],
  d: 'O backend valida CEPs no ViaCEP. Sem internet, os endereços ficam PENDENTE e a prontidão da viagem bloqueia.'
});

// =====================================================================
folder('Cadastros e login', 'Contas públicas (sem token) e login. Só /Autenticacao/Entrar emite JWT.');
r({
  m: 'POST', p: '/Escola/Cadastrar', ok: [201], body: {
    nome: 'Escola Teste {{runId}}', cnpj: '{{cnpjEscola}}', admResponsavel: 'Diretora Cláudia',
    usuarioDTO: usuario('emailEscola', 'ESCOLA'),
    enderecoOperacionalDTO: endereco('01001-000', 'Praça da Sé', '100')
  },
  save: { escolaId: 'json.id', escolaUsuarioId: 'json.usuarioDTO.id' },
  checks: [['Escola criada com id', 'json && json.id']]
});
r({
  m: 'POST', p: '/Motorista/Cadastrar', ok: [201], body: motoristaBody('emailMotorista', 'cpfMotorista'),
  save: { motoristaId: 'json.id', motoristaUsuarioId: 'json.usuarioDTO.id' },
  checks: [['Motorista criado com id', 'json && json.id']]
});
r({
  m: 'POST', p: '/Responsavel/Cadastrar', label: 'Responsável A', ok: [201],
  body: respBody('Maria Responsável A', 'emailRespA', 'cpfRespA'),
  save: { respAId: 'json.id', respAUsuarioId: 'json.usuario.id' },
  checks: [['Responsável A criado', 'json && json.id']]
});
r({
  m: 'POST', p: '/Responsavel/Cadastrar', label: 'Responsável B', ok: [201],
  body: respBody('Ana Responsável B', 'emailRespB', 'cpfRespB'),
  save: { respBId: 'json.id', respBUsuarioId: 'json.usuario.id' },
  checks: [['Responsável B criado', 'json && json.id']]
});
r({ m: 'POST', p: '/Motorista/Cadastrar', label: '(negativo) e-mail duplicado', ok: [400], body: motoristaBody('emailMotorista', 'cpfExtra') });
r({
  m: 'POST', p: '/Motorista/Cadastrar', label: '(negativo) CPF inválido', ok: [400],
  body: Object.assign(motoristaBody('emailExtra', 'cpfExtra'), { cpf: '111.111.111-11' })
});
r({
  m: 'POST', p: '/Responsavel/Cadastrar', label: '(negativo) senha com menos de 12 caracteres', ok: [400],
  body: Object.assign(respBody('Teste Senha Curta', 'emailExtra', 'cpfExtra'), {
    usuario: Object.assign(usuario('emailExtra', 'RESPONSAVEL'), { senha: 'curta123' })
  })
});
r({
  m: 'POST', p: '/Responsavel/Cadastrar', label: '(negativo) termos não aceitos', ok: [400],
  body: Object.assign(respBody('Teste Sem Termos', 'emailExtra', 'cpfExtra'), {
    usuario: Object.assign(usuario('emailExtra', 'RESPONSAVEL'), { aceitouTermos: false })
  })
});
login('admin', 'adminEmail', 'adminPassword', 'ADMIN', { adminUsuarioId: 'json.perfil.usuarioId' });
login('escola', 'emailEscola', 'senhaPadrao', 'ESCOLA');
login('motorista', 'emailMotorista', 'senhaPadrao', 'MOTORISTA');
login('respA', 'emailRespA', 'senhaPadrao', 'RESPONSÁVEL A');
login('respB', 'emailRespB', 'senhaPadrao', 'RESPONSÁVEL B');
r({ m: 'POST', p: '/Autenticacao/Entrar', label: '(negativo) senha errada', ok: [401], body: { email: '{{emailMotorista}}', senha: 'SenhaErrada@123' } });
r({ m: 'GET', p: '/Escola/Perfil', who: 'escola', checks: [['Perfil da escola correto', 'json && String(json.id) === pm.environment.get("escolaId")']] });
r({ m: 'GET', p: '/Motorista/Perfil', who: 'motorista', checks: [['Perfil do motorista correto', 'json && String(json.id) === pm.environment.get("motoristaId")']] });
r({ m: 'GET', p: '/Responsavel/Perfil', label: 'Responsável A', who: 'respA', checks: [['Perfil do responsável correto', 'json && String(json.id) === pm.environment.get("respAId")']] });
r({ m: 'POST', p: '/Escola/Autenticar', label: 'legado, retorna só o perfil', body: { email: '{{emailEscola}}', senha: '{{senhaPadrao}}' } });
r({ m: 'POST', p: '/Motorista/Autenticar', label: 'legado, retorna só o perfil', body: { email: '{{emailMotorista}}', senha: '{{senhaPadrao}}' } });
r({ m: 'POST', p: '/Responsavel/Autenticar', label: 'legado, retorna só o perfil', body: { email: '{{emailRespA}}', senha: '{{senhaPadrao}}' } });

// =====================================================================
folder('Perfis (AlterarInfos)', 'Alteração de dados cadastrais. Exige a senha atual no corpo e os IDs de perfil e de usuário.');
r({
  m: 'PUT', p: '/Motorista/AlterarInfos', who: 'motorista',
  body: '{\n  "id": {{motoristaId}},\n  "nome": "João Motorista Teste (atualizado)",\n  "dataNascimento": "1985-05-20",\n  "cpf": "{{cpfMotorista}}",\n  "usuarioDTO": {\n    "id": {{motoristaUsuarioId}},\n    "email": "{{emailMotorista}}",\n    "senha": "{{senhaPadrao}}",\n    "endereco": "Avenida Paulista, 1000 - Bela Vista, São Paulo/SP",\n    "telefone": "(11) 91234-5678",\n    "tipoPerfil": "MOTORISTA"\n  }\n}',
  checks: [['Nome atualizado', 'json && json.nome.indexOf("atualizado") >= 0']],
  d: 'A CNH não é enviada: só pode ser alterada pelo envio de documento revisado pelo ADMIN.'
});
r({
  m: 'PUT', p: '/Escola/AlterarInfos', who: 'escola',
  body: '{\n  "id": {{escolaId}},\n  "nome": "Escola Teste {{runId}}",\n  "cnpj": "{{cnpjEscola}}",\n  "admResponsavel": "Diretora Cláudia Souza",\n  "usuarioDTO": {\n    "id": {{escolaUsuarioId}},\n    "email": "{{emailEscola}}",\n    "senha": "{{senhaPadrao}}",\n    "endereco": "Praça da Sé, 100 - São Paulo/SP",\n    "telefone": "(11) 93333-4444",\n    "tipoPerfil": "ESCOLA"\n  }\n}',
  checks: [['Responsável da escola atualizado', 'json && json.admResponsavel === "Diretora Cláudia Souza"']]
});
r({
  m: 'PUT', p: '/Responsavel/AlterarInfos', label: 'Responsável A', who: 'respA',
  body: '{\n  "id": {{respAId}},\n  "nome": "Maria Responsável A Silva",\n  "cpf": "{{cpfRespA}}",\n  "dataNascimento": "1990-10-15",\n  "usuario": {\n    "id": {{respAUsuarioId}},\n    "email": "{{emailRespA}}",\n    "senha": "{{senhaPadrao}}",\n    "endereco": "Avenida Paulista, 1000 - Bela Vista, São Paulo/SP",\n    "telefone": "(11) 95555-6666",\n    "tipoPerfil": "RESPONSAVEL"\n  }\n}',
  checks: [['Nome atualizado', 'json && json.nome === "Maria Responsável A Silva"']]
});

// =====================================================================
folder('Motorista: veículo e documentos', 'O veículo vem antes dos documentos: alterar o veículo invalida vistoria e licenciamento. O ADMIN revisa cada documento.');
const veiculo = (cor) => ({ modelo: 'Mercedes-Benz Sprinter', placa: '{{placaVeiculo}}', ano: 2022, capacidade: 15, cor });
r({
  m: 'POST', p: '/Motorista/CadastrarVeiculo/{{motoristaId}}', who: 'motorista', body: veiculo('Branca'),
  checks: [['Veículo vinculado ao motorista', 'json && json.veiculoDTO && json.veiculoDTO.id']]
});
r({
  m: 'PUT', p: '/Motorista/{{motoristaId}}/Veiculo', who: 'motorista', body: veiculo('Prata'),
  checks: [['Cor alterada', 'json && json.veiculoDTO.cor === "Prata"']]
});
r({
  m: 'POST', p: '/Motorista/{{motoristaId}}/Documentos', label: 'CNH', who: 'motorista', ok: [201],
  body: { tipo: 'CNH', numero: '{{cnhNumero}}', categoria: 'D', dataValidade: '{{dataValidade}}' },
  save: { docCnhId: 'json.id' }, checks: [['Documento PENDENTE', 'json && json.status === "PENDENTE"']]
});
r({
  m: 'POST', p: '/Motorista/{{motoristaId}}/Documentos', label: 'VISTORIA', who: 'motorista', ok: [201],
  body: { tipo: 'VISTORIA', numero: 'VIST-{{runId}}', dataRealizacao: '{{dataRealizacao}}', dataValidade: '{{dataValidade}}' },
  save: { docVistoriaId: 'json.id' }, checks: [['Documento PENDENTE', 'json && json.status === "PENDENTE"']]
});
r({
  m: 'POST', p: '/Motorista/{{motoristaId}}/Documentos', label: 'LICENCIAMENTO (RENAVAM)', who: 'motorista', ok: [201],
  body: { tipo: 'LICENCIAMENTO', numero: '{{renavam}}', dataValidade: '{{dataValidade}}' },
  save: { docLicenciamentoId: 'json.id' }, checks: [['Documento PENDENTE', 'json && json.status === "PENDENTE"']]
});
r({ m: 'GET', p: '/Motorista/{{motoristaId}}/Documentos', who: 'motorista', checks: [['Três documentos enviados', 'json && json.length >= 3']] });
r({
  m: 'GET', p: '/Admin/Documentos/Pendentes', who: 'admin',
  checks: [['Fila contém a CNH enviada', 'json && json.some(function (d) { return String(d.id) === pm.environment.get("docCnhId"); })']]
});
r({
  m: 'PUT', p: '/Admin/Documentos/{{docCnhId}}/Revisar', label: 'RECUSAR CNH', who: 'admin',
  body: { status: 'RECUSADO', motivoRecusa: 'Foto ilegível no teste do fluxo' },
  checks: [['CNH recusada', 'json && json.status === "RECUSADO"']]
});
r({
  m: 'POST', p: '/Motorista/{{motoristaId}}/Documentos', label: 'CNH (reenvio após recusa)', who: 'motorista', ok: [201],
  body: { tipo: 'CNH', numero: '{{cnhNumero}}', categoria: 'D', dataValidade: '{{dataValidade}}' },
  save: { docCnhId: 'json.id' }, checks: [['Novo documento PENDENTE', 'json && json.status === "PENDENTE"']]
});
r({
  m: 'PUT', p: '/Admin/Documentos/{{docCnhId}}/Revisar', label: 'APROVAR CNH', who: 'admin', body: { status: 'APROVADO' },
  checks: [['CNH aprovada', 'json && json.status === "APROVADO"']]
});
r({
  m: 'PUT', p: '/Admin/Documentos/{{docVistoriaId}}/Revisar', label: 'APROVAR VISTORIA', who: 'admin', body: { status: 'APROVADO' },
  checks: [['Vistoria aprovada', 'json && json.status === "APROVADO"']]
});
r({
  m: 'PUT', p: '/Admin/Documentos/{{docLicenciamentoId}}/Revisar', label: 'APROVAR LICENCIAMENTO', who: 'admin', body: { status: 'APROVADO' },
  checks: [['Licenciamento aprovado', 'json && json.status === "APROVADO"']]
});
r({
  m: 'GET', p: '/Motorista/{{motoristaId}}/Documentos', label: 'conferir aprovações', who: 'motorista',
  checks: [['Documentos vigentes aprovados', 'json && json.filter(function (d) { return !d.substituidoEm; }).length === 3 && json.filter(function (d) { return !d.substituidoEm; }).every(function (d) { return d.status === "APROVADO"; })']]
});

// =====================================================================
folder('Vínculo motorista x escola', 'Dois caminhos: convite da escola (recusado de propósito) e solicitação do motorista (aprovada). A viagem só fica pronta com vínculo ATIVO.');
r({
  m: 'POST', p: '/Escola/Vinculos/Motoristas/Buscar', label: 'por e-mail', who: 'escola', body: { email: '{{emailMotorista}}' },
  checks: [['Motorista localizado', 'json && json.length === 1']]
});
r({
  m: 'POST', p: '/Escola/Vinculos/Motoristas/Buscar', label: 'por CPF', who: 'escola', body: { cpf: '{{cpfMotorista}}' },
  checks: [['Motorista localizado', 'json && json.length === 1']]
});
r({
  m: 'POST', p: '/Escola/Vinculos', label: 'convidar motorista', who: 'escola', ok: [201], body: '{ "motoristaId": {{motoristaId}} }',
  save: { vinculoId: 'json.id' }, checks: [['Status PENDENTE_MOTORISTA', 'json && json.status === "PENDENTE_MOTORISTA"']]
});
r({ m: 'GET', p: '/Motorista/Vinculos/Pendentes', who: 'motorista', checks: [['Convite pendente listado', 'json && json.length >= 1']] });
r({
  m: 'POST', p: '/Motorista/Vinculos/{{vinculoId}}/Recusar', who: 'motorista', body: { motivo: 'Recusa de teste do fluxo' },
  checks: [['Status RECUSADO', 'json && json.status === "RECUSADO"']]
});
r({
  m: 'GET', p: '/Motorista/Escolas', label: 'buscar por CNPJ', who: 'motorista', q: { termo: '{{cnpjEscola}}' },
  checks: [['Escola encontrada', 'json && json.length === 1']]
});
r({
  m: 'POST', p: '/Motorista/Escolas/Vinculos', label: 'solicitar vínculo', who: 'motorista', ok: [201], body: { cnpj: '{{cnpjEscola}}' },
  save: { vinculoId: 'json.id' }, checks: [['Status PENDENTE_ESCOLA', 'json && json.status === "PENDENTE_ESCOLA"']]
});
r({ m: 'GET', p: '/Escola/Vinculos/Pendentes', who: 'escola', checks: [['Solicitação pendente listada', 'json && json.length >= 1']] });
r({
  m: 'POST', p: '/Escola/Vinculos/{{vinculoId}}/Aprovar', who: 'escola',
  checks: [['Status ATIVO', 'json && json.status === "ATIVO"']]
});
r({ m: 'GET', p: '/Escola/Vinculos', who: 'escola', checks: [['Vínculos da escola', 'json && json.length >= 1']] });
r({ m: 'GET', p: '/Motorista/Vinculos', who: 'motorista', checks: [['Vínculos do motorista', 'json && json.length >= 1']] });
r({ m: 'GET', p: '/Escola/Vinculos/{{vinculoId}}/Historico', who: 'escola', checks: [['Histórico preenchido', 'json && json.length >= 1']] });
r({ m: 'GET', p: '/Motorista/Vinculos/{{vinculoId}}/Historico', who: 'motorista', checks: [['Histórico preenchido', 'json && json.length >= 1']] });
r({ m: 'POST', p: '/Escola/Vinculos', label: '(negativo) par já tem vínculo ativo', who: 'escola', ok: [409], body: '{ "motoristaId": {{motoristaId}} }' });

// =====================================================================
folder('Responsável: dependentes, escola e endereços', 'Cadastra dois dependentes (um para presença online e outro para offline), vincula à escola, cria endereços e testa a rede de responsáveis.');
const dep = (nome, cpfVar, nasc, num) => ({
  nome, cpf: '{{' + cpfVar + '}}', dataNascimento: nasc, periodo: 'MANHA', endereco: 'Avenida Paulista, ' + num + ' - São Paulo/SP'
});
r({ m: 'POST', p: '/Responsavel/CadastrarDependente/{{respAId}}', label: 'Enzo', who: 'respA', body: dep('Enzo Teste', 'cpfDep1', '2016-03-15', '1500') });
r({ m: 'POST', p: '/Responsavel/CadastrarDependente/{{respAId}}', label: 'Sofia', who: 'respA', body: dep('Sofia Teste', 'cpfDep2', '2014-08-20', '2000') });
r({
  m: 'GET', p: '/Responsavel/Perfil', label: 'captura IDs dos dependentes', who: 'respA',
  save: {
    dep1Id: 'json.dependentes.filter(function (d) { return d.nome === "Enzo Teste"; })[0].id',
    dep2Id: 'json.dependentes.filter(function (d) { return d.nome === "Sofia Teste"; })[0].id'
  },
  checks: [['Dois dependentes', 'json && json.dependentes.length === 2']]
});
r({
  m: 'GET', p: '/Responsavel/ListarEscolas', who: 'respA',
  checks: [['Escola de teste listada', 'json && json.some(function (e) { return String(e.id) === pm.environment.get("escolaId"); })']]
});
r({
  m: 'POST', p: '/Responsavel/VincularEscola/{{respAId}}/{{escolaId}}/{{dep1Id}}', label: 'Enzo', who: 'respA'
});
r({
  m: 'POST', p: '/Responsavel/VincularEscola/{{respAId}}/{{escolaId}}/{{dep2Id}}', label: 'Sofia', who: 'respA'
});
r({
  m: 'POST', p: '/Responsavel/Dependente/{{dep1Id}}/Enderecos', label: 'Enzo, AMBOS', who: 'respA', ok: [201],
  body: enderecoDep('1500', 'AMBOS'), save: { end1Id: 'json.id' }, checks: [['Endereço criado', 'json && json.id']],
  d: 'O CEP é validado no ViaCEP. O ideal é statusValidacao VALIDADO.'
});
r({
  m: 'PUT', p: '/Responsavel/Dependente/{{dep1Id}}/Enderecos/{{end1Id}}', label: 'Enzo, alterar número', who: 'respA',
  body: enderecoDep('1600', 'AMBOS'), checks: [['Número alterado', 'json && json.numero === "1600"']]
});
r({
  m: 'POST', p: '/Responsavel/Dependente/{{dep2Id}}/Enderecos', label: 'Sofia, EMBARQUE', who: 'respA', ok: [201],
  body: enderecoDep('2000', 'EMBARQUE'), save: { end2Id: 'json.id' }, checks: [['Endereço criado', 'json && json.id']]
});
r({
  m: 'POST', p: '/Responsavel/Dependente/{{dep2Id}}/Enderecos', label: 'Sofia, DESEMBARQUE', who: 'respA', ok: [201],
  body: enderecoDep('2100', 'DESEMBARQUE'), save: { end2bId: 'json.id' }, checks: [['Endereço criado', 'json && json.id']]
});
r({ m: 'DELETE', p: '/Responsavel/Dependente/{{dep2Id}}/Enderecos/{{end2bId}}', label: 'desativar DESEMBARQUE', who: 'respA', ok: [204] });
r({
  m: 'POST', p: '/Responsavel/Dependente/{{dep2Id}}/Enderecos', label: '(negativo) finalidade duplicada', who: 'respA', ok: [409],
  body: enderecoDep('2000', 'EMBARQUE')
});
r({ m: 'GET', p: '/Responsavel/Dependente/{{dep1Id}}/Enderecos', label: 'Enzo', who: 'respA', checks: [['Um endereço ativo', 'json && json.length === 1']] });
r({ m: 'GET', p: '/Responsavel/Dependente/{{dep2Id}}/Enderecos', label: 'Sofia', who: 'respA', checks: [['Um endereço ativo', 'json && json.length === 1']] });
r({
  m: 'PUT', p: '/Responsavel/Dependente/AlterarInfos', label: 'Enzo', who: 'respA',
  body: '{\n  "id": {{dep1Id}},\n  "nome": "Enzo Teste",\n  "cpf": "{{cpfDep1}}",\n  "dataNascimento": "2016-03-15",\n  "periodo": "MANHA",\n  "endereco": "Avenida Paulista, 1600 - São Paulo/SP",\n  "senha": "{{senhaPadrao}}"\n}',
  checks: [['Endereço legado atualizado', 'json && json.endereco.indexOf("1600") >= 0']]
});
r({
  m: 'POST', p: '/Responsavel/Dependente/{{dep1Id}}/Convites', label: 'A convida B (Enzo)', who: 'respA', ok: [201],
  body: { email: '{{emailRespB}}', categoria: 'MAE' }, save: { conviteId: 'json.id' },
  checks: [['Convite PENDENTE', 'json && json.status === "PENDENTE"']]
});
r({ m: 'GET', p: '/Responsavel/Convites/Pendentes', who: 'respB', checks: [['Convite recebido', 'json && json.length >= 1']] });
r({
  m: 'PUT', p: '/Responsavel/Convites/{{conviteId}}/Aceitar', who: 'respB',
  checks: [['Convite ACEITO', 'json && json.status === "ACEITO"']]
});
r({
  m: 'POST', p: '/Responsavel/Dependente/{{dep2Id}}/Convites', label: 'A convida B (Sofia)', who: 'respA', ok: [201],
  body: { email: '{{emailRespB}}', categoria: 'MAE' }, save: { conviteId2: 'json.id' }
});
r({
  m: 'PUT', p: '/Responsavel/Convites/{{conviteId2}}/Recusar', who: 'respB',
  checks: [['Convite RECUSADO', 'json && json.status === "RECUSADO"']]
});
r({ m: 'GET', p: '/Responsavel/Dependente/{{dep1Id}}/Convites', label: 'histórico', who: 'respA', checks: [['Histórico com o convite', 'json && json.length >= 1']] });
r({ m: 'GET', p: '/Responsavel/Dependente/{{dep1Id}}/Responsaveis', who: 'respA', checks: [['Dois responsáveis ativos', 'json && json.length === 2']] });
r({ m: 'GET', p: '/Admin/Dependentes/ResponsavelCriadorPendentes', who: 'admin', checks: [['Lista retornada', 'Array.isArray(json)']] });

// =====================================================================
folder('Viagem e solicitações', 'Itinerário do motorista, pedido de entrada dos dependentes e aceite do responsável. Ao fim, a prontidão deve ficar verdadeira.');
r({
  m: 'POST', p: '/Viagem/CriarViagem/{{motoristaId}}', who: 'motorista', ok: [201], body: { periodo: 'MANHA_IDA' },
  save: { viagemId: 'json.id' }, checks: [['Viagem ativa', 'json && json.ativo === true']]
});
r({ m: 'POST', p: '/Viagem/CriarViagem/{{motoristaId}}', label: '(negativo) período repetido', who: 'motorista', ok: [409], body: { periodo: 'MANHA_IDA' } });
r({ m: 'GET', p: '/Viagem/ListarViagens/{{motoristaId}}', who: 'motorista', checks: [['Viagem listada', 'json && json.length === 1']] });
r({ m: 'GET', p: '/Viagem/Visualizar/{{viagemId}}', who: 'motorista', checks: [['Período MANHA_IDA', 'json && json.periodo === "MANHA_IDA"']] });
r({ m: 'GET', p: '/Viagem/Agenda/{{motoristaId}}', who: 'motorista', checks: [['Agenda retornada', 'Array.isArray(json)']] });
r({
  m: 'GET', p: '/Viagem/Prontidao/{{viagemId}}', label: 'ainda bloqueada', who: 'motorista',
  checks: [['pronta = false (sem dependentes)', 'json && json.pronta === false']]
});
r({
  m: 'GET', p: '/Viagem/DependentesDisponiveis/{{viagemId}}', who: 'motorista',
  checks: [['Enzo e Sofia disponíveis', 'json && json.length >= 2']]
});
r({
  m: 'POST', p: '/Viagem/SolicitarDependente/{{viagemId}}/{{dep1Id}}', label: 'Enzo', who: 'motorista',
  save: { solic1Id: 'json.id' }, checks: [['Solicitação pendente', 'json && json.respondido === false']]
});
r({
  m: 'POST', p: '/Viagem/SolicitarDependente/{{viagemId}}/{{dep2Id}}', label: 'Sofia', who: 'motorista',
  save: { solic2Id: 'json.id' }, checks: [['Solicitação pendente', 'json && json.respondido === false']]
});
r({ m: 'POST', p: '/Viagem/SolicitarDependente/{{viagemId}}/{{dep1Id}}', label: '(negativo) solicitação repetida', who: 'motorista', ok: [409] });
r({ m: 'GET', p: '/Responsavel/ListarSolicitacoes/{{respAId}}', who: 'respA', checks: [['Duas solicitações', 'json && json.length >= 2']] });
r({
  m: 'POST', p: '/Responsavel/ResponderSolicitacao/{{solic1Id}}', label: 'aceitar Enzo', who: 'respA', body: { respondido: false, aceito: true },
  checks: [['Aceita', 'json && json.aceito === true && json.respondido === true']]
});
r({
  m: 'POST', p: '/Responsavel/ResponderSolicitacao/{{solic2Id}}', label: 'aceitar Sofia', who: 'respA', body: { respondido: false, aceito: true },
  checks: [['Aceita', 'json && json.aceito === true && json.respondido === true']]
});
r({
  m: 'GET', p: '/Viagem/Prontidao/{{viagemId}}', label: 'deve estar pronta', who: 'motorista',
  test: 'if (json && json.bloqueios && json.bloqueios.length) { console.log("Bloqueios: " + json.bloqueios.join(" | ")); }',
  checks: [['pronta = true', 'json && json.pronta === true']],
  d: 'Se vier false, os bloqueios aparecem no Console do Postman (CEP PENDENTE por falta de internet, documento, vínculo...).'
});
r({ m: 'GET', p: '/Viagem/Visualizar/{{viagemId}}', label: 'com dependentes', who: 'motorista', checks: [['Dois dependentes', 'json && json.dependentes.length === 2']] });

// =====================================================================
folder('Execução diária', 'Consentimento GPS, preparação da execução, ordem das paradas, início, localização, presenças (online e offline) e finalização.');
r({
  m: 'PUT', p: '/Viagem/Localizacao/Consentimento/Motorista', who: 'motorista', body: { aceito: true },
  checks: [['Consentimento aceito', 'json && json.aceito === true']],
  d: 'Exige GPS_CONSENT_VERSION configurada no backend; sem ela, retorna 503.'
});
r({
  m: 'PUT', p: '/Viagem/Localizacao/Consentimento/Dependente/{{dep1Id}}', label: 'Enzo', who: 'respA', body: { aceito: true },
  checks: [['Consentimento aceito', 'json && json.aceito === true']]
});
r({
  m: 'PUT', p: '/Viagem/Localizacao/Consentimento/Dependente/{{dep2Id}}', label: 'Sofia', who: 'respA', body: { aceito: true },
  checks: [['Consentimento aceito', 'json && json.aceito === true']]
});
r({
  m: 'POST', p: '/Viagem/PrepararViagemDia/{{viagemId}}', who: 'motorista', body: { data: '{{dataHoje}}' },
  save: { viagemDiaId: 'json.id' }, checks: [['Execução PLANEJADA', 'json && json.status === "PLANEJADA"']]
});
r({
  m: 'POST', p: '/Viagem/IniciarViagemDia/{{viagemId}}', label: 'idempotente (hoje)', who: 'motorista',
  checks: [['Mesma execução', 'json && String(json.id) === pm.environment.get("viagemDiaId")']],
  d: 'Apesar do nome, só cria/recupera a execução do dia como PLANEJADA. O início real é AlterarStatusViagemDia.'
});
r({ m: 'GET', p: '/Viagem/VisualizarViagemDia/{{viagemDiaId}}', who: 'motorista', checks: [['Status PLANEJADA', 'json && json.status === "PLANEJADA"']] });
r({ m: 'GET', p: '/Viagem/ConsultarMotorista/{{viagemDiaId}}', label: 'visão do responsável', who: 'respA', checks: [['Dados do motorista e do veículo', 'json && json.nomeMotorista && json.placaVeiculo']] });
r({
  m: 'GET', p: '/Viagem/BuscarParadasViagemDia/{{viagemDiaId}}', who: 'motorista',
  save: { paradasIds: 'JSON.stringify(json.slice().sort(function (a, b) { return a.ordem - b.ordem; }).map(function (p) { return p.id; }))' },
  checks: [['Ao menos 3 paradas (motorista, local, motorista)', 'json && json.length >= 3']]
});
r({
  m: 'PUT', p: '/Viagem/ReordenarParadasViagemDia/{{viagemDiaId}}', who: 'motorista', pre: REORDENAR,
  body: '{ "idsParadas": {{idsParadasReordenadas}} }',
  checks: [['Paradas retornadas', 'Array.isArray(json) && json.length >= 3']],
  d: 'Troca as duas primeiras paradas intermediárias. A primeira e a última (motorista) são fixas.'
});
r({ m: 'GET', p: '/Viagem/HistoricoOrdemParadas/{{viagemDiaId}}', who: 'motorista', checks: [['Histórico de ordem', 'Array.isArray(json)']] });
r({
  m: 'PUT', p: '/Viagem/AlterarStatusViagemDia', label: 'EM_ANDAMENTO', who: 'motorista',
  body: '{ "idViagemDia": {{viagemDiaId}}, "novoStatus": "EM_ANDAMENTO" }',
  checks: [['Status EM_ANDAMENTO', 'json && json.status === "EM_ANDAMENTO"']],
  d: 'Valida a prontidão (veículo, endereços validados, documentos aprovados, vínculo ATIVO, dependentes).'
});
r({
  m: 'GET', p: '/Viagem/Localizacao/Consentimento/Execucao/{{viagemDiaId}}', who: 'motorista',
  checks: [['GPS liberado', 'json && json.podeAtualizar === true']]
});
r({
  m: 'PUT', p: '/Viagem/Localizacao', who: 'motorista',
  pre: "pm.environment.set('instanteAgora', new Date().toISOString());",
  body: '{ "idViagemDia": {{viagemDiaId}}, "latitude": -23.5614, "longitude": -46.6559, "instanteDispositivo": "{{instanteAgora}}" }',
  checks: [['Posição registrada', 'json && json.latitude !== undefined']]
});
r({ m: 'GET', p: '/Viagem/Localizacao/Atual/{{viagemDiaId}}', label: 'visão do responsável', who: 'respA', checks: [['Posição atual', 'json && json.latitude !== undefined']] });
r({
  m: 'GET', p: '/Viagem/Localizacao/Historico/{{viagemDiaId}}', who: 'motorista',
  pre: "pm.environment.set('inicioIso', new Date(Date.now() - 3600000).toISOString()); pm.environment.set('fimIso', new Date().toISOString());",
  q: { inicio: '{{inicioIso}}', fim: '{{fimIso}}', pagina: '0', tamanho: '50' },
  checks: [['Página com ao menos 1 posição', 'json && json.content && json.content.length >= 1']]
});
r({
  m: 'PUT', p: '/Viagem/AlterarStatusPresenca', label: 'Enzo EMBARCADO', who: 'motorista',
  body: '{ "idViagemDia": {{viagemDiaId}}, "idDependente": {{dep1Id}}, "novoStatus": "EMBARCADO" }',
  checks: [['Paradas retornadas', 'Array.isArray(json)']]
});
r({
  m: 'PUT', p: '/Viagem/AlterarStatusPresenca', label: 'Enzo DESEMBARCADO', who: 'motorista',
  body: '{ "idViagemDia": {{viagemDiaId}}, "idDependente": {{dep1Id}}, "novoStatus": "DESEMBARCADO" }',
  checks: [['Paradas retornadas', 'Array.isArray(json)']]
});
const syncBody = '{\n  "eventos": [\n    { "idEvento": "{{eventoUuid1}}", "idViagemDia": {{viagemDiaId}}, "idDependente": {{dep2Id}}, "statusEsperado": "ESPERANDO", "statusNovo": "EMBARCADO", "ocorridoEm": "{{ocorridoEm1}}" },\n    { "idEvento": "{{eventoUuid2}}", "idViagemDia": {{viagemDiaId}}, "idDependente": {{dep2Id}}, "statusEsperado": "EMBARCADO", "statusNovo": "DESEMBARCADO", "ocorridoEm": "{{ocorridoEm2}}" }\n  ]\n}';
r({
  m: 'POST', p: '/Viagem/SincronizarPresencasOffline', label: 'Sofia, 2 eventos', who: 'motorista', pre: SYNC_PRE, body: syncBody,
  checks: [['Dois eventos APLICADOS', 'json && json.resultados.length === 2 && json.resultados.every(function (x) { return x.resultado === "APLICADO"; })']]
});
r({
  m: 'POST', p: '/Viagem/SincronizarPresencasOffline', label: 'reenvio idempotente', who: 'motorista', body: syncBody,
  checks: [['Eventos DUPLICADOS', 'json && json.resultados.every(function (x) { return x.resultado === "DUPLICADO"; })']]
});
r({
  m: 'POST', p: '/Notificacao/Enviar', who: 'motorista', ok: [201],
  body: '{ "titulo": "Aviso do motorista", "mensagem": "Chegando em 5 minutos.", "visto": false, "remetenteId": {{motoristaUsuarioId}}, "destinatarioId": {{respAUsuarioId}} }',
  checks: [['Notificação criada', 'json && json.id']]
});
r({
  m: 'GET', p: '/Notificacao/Listar/{{respAUsuarioId}}', who: 'respA', save: { notificacaoId: 'json[0].id' },
  checks: [['Há notificações não vistas', 'json && json.length >= 1']]
});
r({ m: 'PUT', p: '/Notificacao/VisualizarNotificacao/{{notificacaoId}}', who: 'respA', ok: [204] });
r({ m: 'GET', p: '/Notificacao/ListarUsuarios', who: 'motorista', checks: [['Usuários listados', 'Array.isArray(json) && json.length >= 1']] });
r({ m: 'GET', p: '/Responsavel/Monitorar/{{dep1Id}}', who: 'respA', checks: [['Status atual do dependente', 'json && json.statusAtual']] });
r({
  m: 'GET', p: '/Viagem/BuscarParadasViagemDia/{{viagemDiaId}}', label: 'status após presenças', who: 'motorista',
  checks: [['Paradas retornadas', 'Array.isArray(json)']]
});
r({
  m: 'PUT', p: '/Viagem/AlterarStatusViagemDia', label: 'FINALIZADA', who: 'motorista',
  body: '{ "idViagemDia": {{viagemDiaId}}, "novoStatus": "FINALIZADA" }',
  checks: [['Status FINALIZADA', 'json && json.status === "FINALIZADA"']],
  d: 'Só finaliza com todas as presenças DESEMBARCADO ou FALTOU.'
});
r({ m: 'GET', p: '/Viagem/HistoricoStatusViagemDia/{{viagemDiaId}}', who: 'motorista', checks: [['Histórico de status', 'json && json.length >= 3']] });
r({ m: 'GET', p: '/Viagem/HistoricoPresenca/{{viagemDiaId}}', who: 'motorista', checks: [['Histórico de presença', 'json && json.length >= 4']] });
r({ m: 'GET', p: '/Viagem/Historico/{{motoristaId}}', who: 'motorista', checks: [['Execuções do motorista', 'Array.isArray(json) && json.length >= 1']] });

// =====================================================================
folder('Execução cancelada', 'Segunda execução (amanhã) para testar cancelamento com motivo obrigatório.');
r({
  m: 'POST', p: '/Viagem/PrepararViagemDia/{{viagemId}}', label: 'amanhã', who: 'motorista', body: { data: '{{dataAmanha}}' },
  save: { viagemDia2Id: 'json.id' }, checks: [['Execução PLANEJADA', 'json && json.status === "PLANEJADA"']]
});
r({
  m: 'PUT', p: '/Viagem/AlterarStatusViagemDia', label: '(negativo) CANCELADA sem motivo', who: 'motorista', ok: [400],
  body: '{ "idViagemDia": {{viagemDia2Id}}, "novoStatus": "CANCELADA" }'
});
r({
  m: 'PUT', p: '/Viagem/AlterarStatusViagemDia', label: 'CANCELADA com motivo', who: 'motorista',
  body: '{ "idViagemDia": {{viagemDia2Id}}, "novoStatus": "CANCELADA", "motivo": "Veículo em manutenção (teste)" }',
  checks: [['Status CANCELADA', 'json && json.status === "CANCELADA"']]
});
r({
  m: 'PUT', p: '/Viagem/AlterarStatusViagemDia', label: '(negativo) transição inválida', who: 'motorista', ok: [409],
  body: '{ "idViagemDia": {{viagemDia2Id}}, "novoStatus": "EM_ANDAMENTO" }'
});

// =====================================================================
folder('Administração', 'Consultas paginadas do ADMIN, atribuição de responsável criador e suspensão/reativação de conta.');
r({
  m: 'GET', p: '/Admin/Usuarios', who: 'admin', q: { tipoPerfil: 'MOTORISTA', email: '{{emailMotorista}}', pagina: '0', tamanho: '10' },
  checks: [['Motorista encontrado', 'json && json.content.length === 1']]
});
r({
  m: 'GET', p: '/Admin/Escolas', who: 'admin', q: { nome: 'Escola Teste {{runId}}', pagina: '0', tamanho: '10' },
  checks: [['Escola encontrada', 'json && json.content.length === 1']]
});
r({
  m: 'GET', p: '/Admin/Vinculos/MotoristaEscola', who: 'admin', q: { escolaId: '{{escolaId}}', pagina: '0', tamanho: '10' },
  checks: [['Vínculos da escola', 'json && json.content.length >= 1']]
});
r({
  m: 'GET', p: '/Admin/Vinculos/ResponsavelDependente', who: 'admin', q: { dependenteId: '{{dep1Id}}', pagina: '0', tamanho: '10' },
  checks: [['Vínculos do dependente', 'json && json.content.length >= 1']]
});
r({
  m: 'GET', p: '/Admin/Auditoria', who: 'admin', q: { acao: 'DOCUMENTO_APROVADO', tipoAlvo: 'DOCUMENTO_MOTORISTA', pagina: '0', tamanho: '10' },
  checks: [['Auditoria de aprovações', 'json && json.content.length >= 1']]
});
r({
  m: 'PUT', p: '/Admin/Dependentes/{{dep1Id}}/ResponsavelCriador', who: 'admin', ok: [204, 400, 404, 409],
  body: '{ "responsavelId": {{respAId}} }',
  d: 'Dependentes cadastrados pela API já têm criador; o retorno pode ser 204 (reatribuído) ou 4xx (já definido). O objetivo é exercitar a rota.'
});
r({ m: 'PUT', p: '/Admin/Usuarios/{{respBUsuarioId}}/Suspender', who: 'admin', ok: [204], body: { motivo: 'Suspensão de teste do fluxo' } });
r({ m: 'POST', p: '/Autenticacao/Entrar', label: '(negativo) conta suspensa', ok: [401], body: { email: '{{emailRespB}}', senha: '{{senhaPadrao}}' } });
r({ m: 'PUT', p: '/Admin/Usuarios/{{respBUsuarioId}}/Reativar', who: 'admin', ok: [204], body: { motivo: 'Reativação de teste' } });
login('respB', 'emailRespB', 'senhaPadrao', 'RESPONSÁVEL B reativado');

// =====================================================================
folder('Encerramentos', 'Suspensão e retomada do vínculo, encerramentos e novos ciclos de vínculo. Rode depois da execução diária.');
r({
  m: 'POST', p: '/Escola/Vinculos/{{vinculoId}}/Suspender', who: 'escola', body: { motivo: 'Suspensão de teste' },
  checks: [['Status SUSPENSO', 'json && json.status === "SUSPENSO"']]
});
r({
  m: 'GET', p: '/Viagem/Prontidao/{{viagemId}}', label: 'bloqueada com vínculo suspenso', who: 'motorista',
  checks: [['pronta = false', 'json && json.pronta === false']]
});
r({
  m: 'POST', p: '/Escola/Vinculos/{{vinculoId}}/Reativar', who: 'escola', body: { motivo: 'Retomada' },
  checks: [['Status ATIVO', 'json && json.status === "ATIVO"']]
});
r({ m: 'GET', p: '/Viagem/Prontidao/{{viagemId}}', label: 'pronta novamente', who: 'motorista', checks: [['pronta = true', 'json && json.pronta === true']] });
r({
  m: 'PUT', p: '/Responsavel/EncerrarVinculoViagem/{{solic2Id}}', label: 'Sofia sai da viagem', who: 'respA',
  checks: [['dataFim preenchida', 'json && json.dataFim']]
});
r({ m: 'PUT', p: '/Responsavel/EncerrarVinculoViagem/{{solic2Id}}', label: '(negativo) já encerrado', who: 'respA', ok: [409] });
r({
  m: 'PUT', p: '/Viagem/DesativarViagem/{{viagemId}}', who: 'motorista',
  checks: [['Mensagem de sucesso', 'json && json.mensagem']]
});
r({
  m: 'GET', p: '/Viagem/Prontidao/{{viagemId}}', label: 'itinerário desativado', who: 'motorista',
  checks: [['pronta = false', 'json && json.pronta === false']]
});
r({ m: 'DELETE', p: '/Responsavel/Dependente/{{dep1Id}}/Responsaveis/{{respBId}}', label: 'revogar B de Enzo', who: 'respA', ok: [204] });
r({
  m: 'POST', p: '/Escola/Vinculos/{{vinculoId}}/Encerrar', who: 'escola', body: { motivo: 'Fim do contrato de teste' },
  checks: [['Status ENCERRADO', 'json && json.status === "ENCERRADO"']]
});
r({
  m: 'POST', p: '/Escola/Vinculos', label: 'novo convite após encerramento', who: 'escola', ok: [201], body: '{ "motoristaId": {{motoristaId}} }',
  save: { vinculo2Id: 'json.id' }
});
r({
  m: 'POST', p: '/Motorista/Vinculos/{{vinculo2Id}}/Aceitar', who: 'motorista',
  checks: [['Status ATIVO', 'json && json.status === "ATIVO"']]
});
r({
  m: 'POST', p: '/Motorista/Vinculos/{{vinculo2Id}}/Encerrar', who: 'motorista', body: { motivo: 'Encerramento pelo motorista (teste)' },
  checks: [['Status ENCERRADO', 'json && json.status === "ENCERRADO"']]
});
r({
  m: 'POST', p: '/Motorista/Escolas/Vinculos', label: 'nova solicitação', who: 'motorista', ok: [201], body: { cnpj: '{{cnpjEscola}}' },
  save: { vinculo3Id: 'json.id' }
});
r({
  m: 'POST', p: '/Escola/Vinculos/{{vinculo3Id}}/Recusar', who: 'escola', body: { motivo: 'Recusa de teste' },
  checks: [['Status RECUSADO', 'json && json.status === "RECUSADO"']]
});

// =====================================================================
folder('Autenticação: sessão e senha', 'Renovação com rotação de refresh token, reuso (derruba a sessão), troca de senha e logout. Usa a conta do motorista.');
login('motorista', 'emailMotorista', 'senhaPadrao', 'nova sessão');
r({
  m: 'POST', p: '/Autenticacao/Renovar', label: 'rotaciona o refresh', ok: [200],
  pre: "pm.environment.set('refreshAnterior', pm.environment.get('motoristaRefresh'));",
  body: '{ "refreshToken": "{{motoristaRefresh}}" }',
  save: { motoristaToken: 'json.accessToken', motoristaRefresh: 'json.refreshToken' },
  checks: [['Novo refresh diferente do anterior', 'json && json.refreshToken !== pm.environment.get("refreshAnterior")']]
});
r({
  m: 'POST', p: '/Autenticacao/Renovar', label: '(negativo) reuso do refresh antigo', ok: [401],
  body: '{ "refreshToken": "{{refreshAnterior}}" }', d: 'Reuso é detectado e a sessão inteira é encerrada.'
});
login('motorista', 'emailMotorista', 'senhaPadrao', 'nova sessão após reuso');
r({
  m: 'POST', p: '/Autenticacao/AlterarSenha', who: 'motorista', ok: [204],
  body: { senhaAtual: '{{senhaPadrao}}', novaSenha: '{{senhaNova}}' }
});
r({ m: 'POST', p: '/Autenticacao/Entrar', label: '(negativo) senha antiga', ok: [401], body: { email: '{{emailMotorista}}', senha: '{{senhaPadrao}}' } });
login('motorista', 'emailMotorista', 'senhaNova', 'senha nova');
r({
  m: 'POST', p: '/Autenticacao/SolicitarRedefinicaoSenha', ok: [202], body: { email: '{{emailMotorista}}' },
  d: 'Resposta genérica. Sem SendGrid configurado, nenhum e-mail é enviado.'
});
r({
  m: 'POST', p: '/Autenticacao/RedefinirSenha', label: '(negativo) token inválido', ok: [401],
  body: { token: 'token-invalido', novaSenha: '{{senhaPadrao}}' },
  d: 'O token real só chega por e-mail (SendGrid).'
});
r({
  m: 'POST', p: '/Autenticacao/Sair', who: 'motorista', ok: [204], body: '{ "refreshToken": "{{motoristaRefresh}}" }'
});
r({
  m: 'POST', p: '/Autenticacao/Renovar', label: '(negativo) refresh após Sair', ok: [401], body: '{ "refreshToken": "{{motoristaRefresh}}" }'
});

// =====================================================================
folder('Autorização (negativos)', 'Confere 401 e 403: sem token, perfil errado, recurso de outro usuário e token inválido.');
r({ m: 'GET', p: '/Admin/Usuarios', label: 'sem token', ok: [401] });
r({ m: 'GET', p: '/Admin/Usuarios', label: 'perfil RESPONSÁVEL', who: 'respA', ok: [403] });
r({ m: 'GET', p: '/Viagem/ListarViagens/{{motoristaId}}', label: 'responsável em rota do motorista', who: 'respA', ok: [403] });
r({ m: 'GET', p: '/Motorista/Perfil', label: 'token de escola', who: 'escola', ok: [403] });
r({ m: 'GET', p: '/Responsavel/Dependente/{{dep1Id}}/Enderecos', label: 'B revogado de Enzo', who: 'respB', ok: [403, 404] });
r({ m: 'PUT', p: '/Admin/Usuarios/{{motoristaUsuarioId}}/Suspender', label: 'token de motorista', who: 'escola', ok: [403], body: { motivo: 'não deveria funcionar' } });

// ---------- Escrita dos arquivos ----------
allScripts.forEach((code, i) => {
  try { new Function('pm', code); } catch (e) { throw new Error('Script inválido #' + i + ': ' + e.message + '\n' + code); }
});

const collection = {
  info: {
    _postman_id: crypto.randomUUID(),
    name: 'Tio da Perua - Roteiro de teste ponta a ponta',
    schema: 'https://schema.getpostman.com/json/collection/v2.1.0/collection.json',
    description: [
      'Roteiro numerado para testar a API local do início ao fim. Execute as pastas na ordem (Collection Runner).',
      '',
      'Como usar:',
      '1. Importe esta coleção e o environment "Tio da Perua - Local" e selecione-o.',
      '2. No environment, preencha adminPassword com o valor de $env:ADMIN_PASSWORD (criado por scripts/dev-env.ps1) e ajuste baseUrl se a porta for outra.',
      '3. Com o backend rodando e DOCS_ENABLED=true, rode a pasta 0 e depois as demais em sequência. Cada execução gera CPF/CNPJ/e-mails novos.',
      '',
      'Requisitos: internet (ViaCEP), GPS_CONSENT_VERSION definida no backend e PostgreSQL ativo.'
    ].join('\n')
  },
  item: folders.map((f) => ({ name: f.name, description: f.description, item: f.items.map(buildItem) })),
};

const envValues = [
  ['baseUrl', 'http://localhost:8090/TioDaPerua/api', 'default'],
  ['adminEmail', 'admin@tiodaperua.local', 'default'],
  ['adminPassword', '', 'secret'],
  ['senhaPadrao', 'SenhaForte@2026', 'default'],
  ['senhaNova', 'NovaSenha@2026!', 'default']
];
const environment = {
  id: crypto.randomUUID(),
  name: 'Tio da Perua - Local',
  values: envValues.map(([key, value, type]) => ({ key, value, type, enabled: true })),
  _postman_variable_scope: 'environment'
};

fs.mkdirSync(OUT, { recursive: true });
fs.writeFileSync(path.join(OUT, 'TioDaPerua.postman_collection.json'), JSON.stringify(collection, null, 2), 'utf8');
fs.writeFileSync(path.join(OUT, 'TioDaPerua.local.postman_environment.json'), JSON.stringify(environment, null, 2), 'utf8');

const total = folders.reduce((s, f) => s + f.items.length, 0);
console.log('Pastas: ' + folders.length + ' | Requisições: ' + total);
folders.forEach((f) => console.log('  ' + f.name + ': ' + f.items.length));
