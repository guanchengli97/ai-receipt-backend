# Ansible Trigger Guide

This guide explains when and how to run Ansible in this repository.

## Scope

Ansible provisions MySQL on the target host. The Java application is deployed separately as a Docker container by GitHub Actions.

## Manual Local Trigger

Use this for first-time setup and database configuration changes.

```bash
cd ansible
bash deploy.sh mysql
```

What it does:

- Installs MySQL and Python MySQL dependencies.
- Starts and enables the MySQL service.
- Creates `ai_receipt_db`.
- Creates `receipt_user`.
- Grants privileges.
- Runs database initialization through `init-database.yml`.

## Direct Playbook Command

```bash
cd ansible
ansible-playbook deploy.yml \
  -i hosts.ini \
  --ask-vault-pass \
  --ask-become-pass
```

Use direct commands when debugging inventory, privilege escalation, or vault issues.

## GitHub Actions Relationship

The active application workflow is `.github/workflows/deploy.yml`.

It does not run Ansible. It:

1. Builds the Java app with Maven.
2. Builds and pushes the Docker image.
3. SSHes into the Oracle host.
4. Pulls the image and restarts `ai-receipt-app`.

## Typical First Deployment

1. Configure `ansible/hosts.ini`.
2. Create `ansible/vars/vault.yml`.
3. Run `bash ansible/deploy.sh mysql`.
4. Verify MySQL on the target host.
5. Add GitHub Actions secrets.
6. Push to `master`.
7. Verify the Docker container and API.

## Typical App Update

```bash
git push origin master
```

No Ansible run is needed for normal application code changes.

## Troubleshooting

```bash
# Test Ansible connectivity
cd ansible
ansible -i hosts.ini all -m ping

# Run with verbose output
ansible-playbook deploy.yml -i hosts.ini --ask-vault-pass --ask-become-pass -vvv

# Check MySQL on target host
ssh ubuntu@<ORACLE_HOST>
sudo systemctl status mysql
mysql -u receipt_user -p ai_receipt_db -e "SELECT 1;"
```

## Notes

If you add an Ansible workflow later, keep it limited to database/server provisioning unless the deployment architecture intentionally changes.
