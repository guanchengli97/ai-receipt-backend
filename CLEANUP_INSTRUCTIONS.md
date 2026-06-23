# Cleanup Instructions

This file documents cleanup tasks after removing the old Ansible `java-app` deployment path.

## Expected Final State

The `ansible/` directory should only provision MySQL and initialize database state.

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
        |-- tasks/
        `-- handlers/
```

There should be no active `ansible/roles/java-app/` deployment role.

## Manual Cleanup

If the old role still exists, remove it:

```bash
# Linux/macOS
rm -rf ansible/roles/java-app/

# Windows PowerShell
Remove-Item -Recurse -Force ansible/roles/java-app
```

## Verification Checklist

- [ ] `ansible/roles/java-app/` is gone.
- [ ] `ansible/deploy.yml` only includes the MySQL role and database initialization.
- [ ] `ansible/deploy.sh` only accepts `mysql`.
- [ ] `ansible/deploy.ps1` only accepts `mysql`.
- [ ] Production application deployment is handled by `.github/workflows/deploy.yml`.
- [ ] Documentation no longer tells users to deploy the Java app through Ansible/systemd.

## Current Deployment Commands

Provision MySQL:

```bash
cd ansible
bash deploy.sh mysql
```

Deploy the app:

```bash
git push origin master
```

## Notes

The cleanup keeps the production model simple: host-level MySQL managed by Ansible, application runtime managed by Docker.
