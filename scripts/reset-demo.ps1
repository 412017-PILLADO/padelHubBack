# Resetea la base demo a un estado limpio: borra el volumen de MySQL y lo recrea.
# Flyway re-aplica las migraciones (schema V1 + seed V2: tenant `demo`, 1 complejo, 3 canchas,
# horarios 08:00-23:00) la proxima vez que arranca el backend; el owner lo recrea OwnerSeeder.
#
# Uso:  powershell -File scripts\reset-demo.ps1
# Luego: arrancar/reiniciar el backend para que corra Flyway + OwnerSeeder.
$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '..')

Write-Host "> Bajando MySQL y borrando el volumen..."
docker compose down -v

Write-Host "> Levantando MySQL limpio..."
docker compose up -d

Write-Host "> Esperando a que MySQL este healthy..."
for ($i = 0; $i -lt 40; $i++) {
  $status = (docker inspect -f '{{.State.Health.Status}}' padel-mysql 2>$null)
  if ($status -eq 'healthy') {
    Write-Host "OK MySQL healthy. Datos demo listos para re-seedear al arrancar el backend."
    exit 0
  }
  Start-Sleep -Seconds 2
}

Write-Error "MySQL no llego a healthy a tiempo."
exit 1
