# ClinicaVet README

## Cronograma
- **Semana 1 - Autenticação e base organizacional**
- [x] V1: Criar tabela de usuários/empresas/vínculos;
- [x] Configurar Datasource e Flyway;
- [x] Implementar SignUp -> email com senha provisória;
- [x] Implementar Primeiro Login (confirmação) -> promove senha provisória para oficial;
- [x] Telas Vaadin: SignUp, Login/Confirm, ForgotPassword (gera nova provisória);
- [x] Serviço de validação de documento (CPF/CNPJ/PASSAPORTE) básico.
#
- **Semana 2 - Empresa e vínculo**
- [x] Telas: CompanyForm/List, UserCompanyView;
- [x] Ao criar empresa, vincular quem criou como admin;
- [x] Listagem de vínculos, criar/remover (soft-delete), checagem de permissões (is_admin);
- [x] Guard no app para exigir vínculo ativo antes de acessar módulos da empresa.
#
- **Semana 3 - Pessoas e Animais**
- [x] V2: tabelas person e animal;
- [ ] Repositórios JDBC e Services com validações (unicidade documento por empresa);
- [x] Telas: PersonList/Form (com busca), AnimalList/Form (escolher tutor).
#
- **Semana 4 - Atendimentos**
- [ ] V3: tabela attendance;
- [ ] Telas: AttendanceList/Form (agendamento e realização);
- [ ] Completar ForgotPassword enviando provisória (SMTP real) e ajustar UX (mensagens, validações);
- [ ] Docker Compose revisado (app + db), README atualizado.

## Regras de Negócio

- Usuário (autocadastro e confirmação)
```
* Autocadastro: usuário informa email → sistema cria app_user com provisional_password_hash preenchida (hash da senha temporária gerada) e password_hash vazio; envia email com a senha provisória.
* Primeiro login/Confirmação:
  Se a senha informada bate no provisional_password_hash, então:
    1.	copiar o hash para password_hash (ou re-hashear com BCrypt),
    2.	limpar provisional_password_hash,
    3.	marcar email_confirmed_at = NOW().
  Senão, validar direto em password_hash (para logins futuros).
* Esqueci minha senha: gera nova senha temporária → grava no provisional_password_hash → envia por email. No próximo login com essa senha, promove-a para password_hash e limpa provisional_password_hash.

Importante: Sempre usar hash (ex.: BCrypt) para ambos os campos; nunca armazenar senha em texto puro.
```

- Empresa e vínculo
```
* Um usuário só pode operar (cadastrar pessoas/animais/atendimentos) se possuir vínculo ativo (user_company com deleted_at IS NULL) com alguma empresa.
* Admin no vínculo (is_admin = TRUE) pode registrar novos vínculos para a mesma empresa.
* Unicidade user_id + company_id evita duplicidade de vínculo.
```

- Pessoas/Animais
```
* Pessoa sempre pertence a uma empresa. Todos os usuários vinculados àquela empresa têm acesso aos dados (conforme requisito).
* Animal pertence a uma pessoa (tutor).
* Unicidade de documento dentro da empresa (índice parcial) para evitar duplicidade.
```

- Atendimentos (agendamento → realização)
```
* Pode criar um agendamento futuro com scheduled_at.
* Ao realizar o atendimento, preencha appointment_at (e mantenha scheduled_at para histórico).
```

## Desenvolvimento
## Telas

```
* Públicas (sem vínculo necessário):
  1. SignUpView: campo email → “Criar conta” (gera senha provisória, envia email).
  2. ConfirmView / Primeiro Login: email + senha (provisória) → promove para oficial.
  3. ForgotPasswordView: email → gera nova provisória e envia.

* Protegidas (exigem vínculo com uma empresa):
  4. CompanyListView / CompanyForm: CRUD de empresas (visível a admins globais ou via fluxo “criar empresa” e já vincular o criador como admin).
  5. UserCompanyView: listar vínculos da empresa; criar novo vínculo (admin); soft-delete de vínculo.
  6. PersonListView / PersonForm: CRUD de pessoas da empresa; busca por nome/documento/telefone.
  7. AnimalListView / AnimalForm: CRUD de animais vinculados ao tutor.
  8. AttendanceListView / AttendanceForm: criar agendamento (scheduled_at), realizar (appointment_at), descrição.
```
## Validações essenciais (service)
```
• Email único em app_user.
• Senha: sempre hash (BCrypt).
• Documento: validar por doc_type (inicio simples, ex.: máscara/quantidade de dígitos).
• Vínculo ativo: checar deleted_at IS NULL antes de permitir operações na empresa.
• Pessoas: impedir duplicidade (company_id, doc_type, document).
• Atendimento: para “agendar”, exigir scheduled_at; para “realizar”, exigir appointment_at.
```
