# Ansible Deployment Guide

Ansible in this repository provisions MySQL for AI Receipt Backend. It does not deploy the Java application.

## Current Scope

Ansible handles:

- MySQL package installation.
- MySQL service startup.
- Database creation.
- Application user creation.
- Privilege grants.
- Optional database initialization through `init-database.yml`.

GitHub Actions handles:

- Maven build.
- Docker image build and push.
- Remote container restart on the Oracle host.

## File Layout

```text
ansible/
|-- deploy.yml
|-- deploy.sh
|-- deploy.ps1
|-- hosts.ini
|-- hosts.ini.example
|-- init-database.yml
|-- vars/
|   |-- main.yml
|   `-- vault.yml
`-- roles/
    `-- mysql/
        |-- tasks/main.yml
        `-- handlers/main.yml
```

## Prerequisites

- Ansible installed on the control machine.
- SSH access to the target server.
- Target user has sudo access.
- `ansible/hosts.ini` configured.
- `ansible/vars/vault.yml` created.

## Inventory

Example `ansible/hosts.ini`:

```ini
[webservers]
oracle-prod ansible_host=<ORACLE_HOST> ansible_user=ubuntu ansible_ssh_private_key_file=~/.ssh/oracle_key
```

Test:

```bash
cd ansible
ansible -i hosts.ini all -m ping
```

## Vault Values

Create encrypted secrets:

```bash
cd ansible
ansible-vault create vars/vault.yml
```

Example:

```yaml
vault_mysql_root_password: "strong-root-password"
vault_db_password: "strong-receipt-user-password"
vault_jwt_secret: "long-random-jwt-secret"
```

## Non-Secret Values

Edit `ansible/vars/main.yml` for database names and general settings:

```yaml
db_host: "localhost"
db_port: 3306
db_name: "ai_receipt_db"
db_user: "receipt_user"
db_password: "{{ vault_db_password | default('') }}"
```

## Run Deployment

Recommended helper:

```bash
cd ansible
bash deploy.sh mysql
```

PowerShell helper:

```powershell
cd ansible
.\deploy.ps1 -DeploymentType mysql
```

Direct playbook:

```bash
cd ansible
ansible-playbook deploy.yml \
  -i hosts.ini \
  --ask-vault-pass \
  --ask-become-pass
```

## Verify

```bash
ssh ubuntu@<ORACLE_HOST>
sudo systemctl status mysql
mysql -u receipt_user -p ai_receipt_db -e "SELECT 1;"
```

## Troubleshooting

Vault password error:

```bash
ansible-vault edit ansible/vars/vault.yml
ansible-vault rekey ansible/vars/vault.yml
```

SSH failure:

```bash
cd ansible
ansible -i hosts.ini all -m ping -vvv
```

Privilege failure:

```bash
ansible-playbook deploy.yml -i hosts.ini --ask-vault-pass --ask-become-pass -vvv
```

MySQL failure:

```bash
ssh ubuntu@<ORACLE_HOST>
sudo systemctl status mysql
sudo tail -f /var/log/mysql/error.log
```

## Security Notes

- Keep `vars/vault.yml` encrypted.
- Do not commit plaintext database passwords.
- Use SSH keys, not password-based SSH.
- Restrict MySQL and application ports at the cloud firewall level.
- Rotate credentials after accidental exposure.

## Related Docs

- `ansible/DEPLOY_GUIDE.md`
- `ansible/ARCHITECTURE.md`
- `ORACLE_DEPLOYMENT.md`
