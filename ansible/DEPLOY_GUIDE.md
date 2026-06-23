# Ansible Deploy Guide

This guide covers the Ansible files under `ansible/`.

## Purpose

The Ansible playbook provisions MySQL for the backend. The application itself is deployed by Docker through GitHub Actions.

## Directory Layout

```text
ansible/
|-- deploy.yml                 # Main MySQL playbook
|-- init-database.yml          # Optional database initialization
|-- deploy.sh                  # Linux/macOS helper
|-- deploy.ps1                 # Windows PowerShell helper
|-- hosts.ini                  # Local inventory
|-- hosts.ini.example          # Inventory example
|-- vars/
|   |-- main.yml               # Non-secret variables
|   `-- vault.yml              # Encrypted secrets
`-- roles/
    `-- mysql/
        |-- tasks/main.yml
        `-- handlers/main.yml
```

## Configure Inventory

```ini
[webservers]
oracle-prod ansible_host=<ORACLE_HOST> ansible_user=ubuntu ansible_ssh_private_key_file=~/.ssh/oracle_key
```

Test inventory:

```bash
ansible -i hosts.ini all -m ping
```

## Configure Secrets

Create or edit the vault:

```bash
ansible-vault create vars/vault.yml
ansible-vault edit vars/vault.yml
```

Required values:

```yaml
vault_mysql_root_password: "strong-root-password"
vault_db_password: "strong-receipt-user-password"
vault_jwt_secret: "long-random-jwt-secret"
```

## Run the Playbook

Helper script:

```bash
bash deploy.sh mysql
```

Direct command:

```bash
ansible-playbook deploy.yml \
  -i hosts.ini \
  --ask-vault-pass \
  --ask-become-pass
```

Dry run:

```bash
ansible-playbook deploy.yml \
  -i hosts.ini \
  --ask-vault-pass \
  --ask-become-pass \
  --check
```

Verbose run:

```bash
ansible-playbook deploy.yml \
  -i hosts.ini \
  --ask-vault-pass \
  --ask-become-pass \
  -vvv
```

## What the MySQL Role Does

- Updates package indexes on Debian-based hosts.
- Installs MySQL server/client packages.
- Installs Python MySQL dependencies.
- Starts and enables MySQL.
- Creates the application database.
- Creates `receipt_user` for localhost and remote access.
- Grants privileges on `ai_receipt_db`.
- Flushes privileges.
- Configures MySQL bind address for remote access.

## Variables

Important non-secret variables in `vars/main.yml`:

| Variable | Default | Purpose |
| --- | --- | --- |
| `db_host` | `localhost` | Database host |
| `db_port` | `3306` | Database port |
| `db_name` | `ai_receipt_db` | Application database |
| `db_user` | `receipt_user` | Application database user |
| `db_password` | `{{ vault_db_password }}` | Password sourced from vault |

## Verification

```bash
ssh ubuntu@<ORACLE_HOST>
sudo systemctl status mysql
mysql -u receipt_user -p ai_receipt_db -e "SELECT DATABASE(), NOW();"
```

## Common Issues

### Vault password is wrong

Use the correct vault password or rekey the file:

```bash
ansible-vault rekey vars/vault.yml
```

### SSH cannot connect

```bash
ansible -i hosts.ini all -m ping -vvv
```

Check host, user, key path, SSH port, and cloud firewall rules.

### Sudo password is required

Run with:

```bash
ansible-playbook deploy.yml -i hosts.ini --ask-vault-pass --ask-become-pass
```

### MySQL user cannot connect

```bash
sudo mysql -u root -e "SELECT User, Host FROM mysql.user WHERE User='receipt_user';"
mysql -u receipt_user -p ai_receipt_db -e "SELECT 1;"
```

Ensure the password matches `vault_db_password`.

## Cleanup

Do not remove MySQL data unless you have a backup.

```bash
# Stop MySQL
sudo systemctl stop mysql

# Backup before destructive changes
mysqldump -u receipt_user -p ai_receipt_db > ai_receipt_db_backup.sql
```

Destructive cleanup, only when intentionally removing the database:

```sql
DROP DATABASE ai_receipt_db;
DROP USER 'receipt_user'@'localhost';
DROP USER 'receipt_user'@'%';
FLUSH PRIVILEGES;
```
