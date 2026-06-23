# Ansible Java-App Cleanup Notes

The old Ansible `java-app` deployment path has been removed from the intended architecture.

## Current Deployment Split

```text
MySQL provisioning -> Ansible
Application deploy -> Docker + GitHub Actions
```

Ansible should only prepare the database layer. The Spring Boot application is built into a Docker image and deployed by `.github/workflows/deploy.yml`.

## Removed or Deprecated Items

- `ansible/roles/java-app/`
- `java-app` deployment options in Ansible helper scripts
- Java application deployment through systemd
- Documentation that instructed Ansible to build or run the Spring Boot JAR directly

## Retained Items

- `ansible/deploy.yml`
- `ansible/deploy.sh`
- `ansible/deploy.ps1`
- `ansible/init-database.yml`
- `ansible/roles/mysql/`
- `ansible/vars/main.yml`
- `ansible/vars/vault.yml`

## Current Flow

1. Provision MySQL:

```bash
cd ansible
bash deploy.sh mysql
```

2. Deploy the app:

```bash
git push origin master
```

GitHub Actions builds and deploys the Docker container.

## Recovery

If direct Ansible Java deployment is needed again later, recover the old `java-app` role from Git history and update the documentation, scripts, and workflows together. Do not mix both deployment paths without an explicit reason.
