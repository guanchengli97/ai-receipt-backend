# Ansible Architecture

This document describes the Ansible part of the deployment architecture.

## Responsibility Boundary

```text
Ansible
  -> provision MySQL on the target host

GitHub Actions + Docker
  -> build and deploy the Spring Boot application
```

The old direct Java deployment role is no longer part of the active architecture.

## Control and Target Nodes

```text
Control node
  -> local developer machine or CI runner
  -> runs ansible-playbook
  -> reads hosts.ini, vars/main.yml, vars/vault.yml
  -> connects over SSH

Target node
  -> Oracle/Linux host
  -> runs MySQL
  -> later runs Docker container deployed by GitHub Actions
```

## Ansible File Flow

```text
deploy.yml
  -> loads vars/vault.yml
  -> loads vars/main.yml
  -> runs roles/mysql
  -> imports init-database.yml
```

## MySQL Role Flow

```text
1. Update package cache
2. Install MySQL packages
3. Install Python MySQL dependencies
4. Start and enable MySQL
5. Create ai_receipt_db
6. Create receipt_user
7. Grant database privileges
8. Configure bind address
9. Restart MySQL when config changes
```

## Runtime Architecture After Full Deployment

```text
Oracle host
|-- MySQL
|   |-- service: mysql
|   |-- port: 3306
|   |-- database: ai_receipt_db
|   `-- user: receipt_user
`-- Docker
    `-- ai-receipt-app
        |-- image: docker.io/<docker-user>/ai-receipt-backend:latest
        |-- host port: 7008
        |-- container port: 8080
        `-- database URL: jdbc:mysql://host.docker.internal:3306/ai_receipt_db
```

## Secrets Flow

```text
Ansible vault
  vault_db_password
    -> creates/updates MySQL receipt_user password

GitHub Actions secret
  DB_PASSWORD
    -> passed into Docker container as SPRING_DATASOURCE_PASSWORD
```

These values must match.

## Network Flow

```text
Browser/client
  -> http://<ORACLE_HOST>:7008/api/
Oracle host Docker port mapping
  -> container port 8080
Spring Boot app
  -> host.docker.internal:3306
MySQL on host
```

The GitHub Actions deployment uses Docker's `host-gateway` mapping so the container can reach host MySQL through `host.docker.internal`.

## Operational Commands

Check Ansible connectivity:

```bash
cd ansible
ansible -i hosts.ini all -m ping
```

Run provisioning:

```bash
bash deploy.sh mysql
```

Check MySQL:

```bash
ssh ubuntu@<ORACLE_HOST>
sudo systemctl status mysql
mysql -u receipt_user -p ai_receipt_db -e "SELECT 1;"
```

Check the app container:

```bash
docker ps
docker logs -f ai-receipt-app
curl http://localhost:7008/api/
```

## Failure Points

| Area | Common failure | Diagnostic command |
| --- | --- | --- |
| SSH | Bad host, user, key, or port | `ansible -i hosts.ini all -m ping -vvv` |
| Vault | Wrong vault password | `ansible-vault view vars/vault.yml` |
| Sudo | Target user lacks privilege | `ansible-playbook ... --ask-become-pass -vvv` |
| MySQL | Service failed or bad password | `sudo systemctl status mysql` |
| Docker app | Missing env var or DB connection failure | `docker logs ai-receipt-app` |

## Design Rationale

Keeping MySQL on the host and the app in Docker separates persistent data from application releases. Ansible changes are infrequent and explicit, while application releases can be rebuilt and redeployed automatically on each push to `master`.
