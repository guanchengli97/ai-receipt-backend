# Ansible Trigger Summary

Ansible is used for MySQL provisioning. It does not deploy the Spring Boot application.

## Recommended Trigger

Run Ansible manually from your local machine for first-time server setup:

```bash
cd ansible
bash deploy.sh mysql
```

This is preferred because initial database setup is infrequent and easier to debug with direct terminal output.

## Application Deployment Trigger

Application deployment is automatic on pushes to `master`:

```bash
git push origin master
```

The GitHub Actions workflow builds the app, pushes a Docker image, and restarts the container on the Oracle host.

## Comparison

| Task | Tool | Trigger | Frequency |
| --- | --- | --- | --- |
| Install/configure MySQL | Ansible | Manual command | First setup or DB changes |
| Build Docker image | GitHub Actions | Push to `master` | Every app release |
| Restart app container | GitHub Actions over SSH | Push to `master` | Every app release |

## Required Local Setup for Ansible

- Ansible installed
- SSH access to the target server
- `ansible/hosts.ini` configured
- `ansible/vars/vault.yml` created and encrypted

## Required GitHub Secrets for App Deployment

At minimum:

```text
DOCKER_USERNAME
DOCKER_PASSWORD
ORACLE_HOST
ORACLE_USERNAME
SSH_PRIVATE_KEY
DB_PASSWORD
JWT_SECRET
```

The current workflow also expects secrets for Google, Gemini, S3, mail, and Stripe features.
