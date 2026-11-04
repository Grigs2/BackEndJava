# Uso: . .\scripts\dev-env.ps1   (com o ponto, para carregar as variaveis no terminal atual)
# Cria uma vez cada variavel de desenvolvimento e a grava no ambiente do usuario do Windows,
# entao a chave JWT fica fixa entre execucoes. Nada e gravado no repositorio.

function Set-DevVar([string]$Name, [string]$Value) {
    $current = [Environment]::GetEnvironmentVariable($Name, 'User')
    if ([string]::IsNullOrWhiteSpace($current)) {
        [Environment]::SetEnvironmentVariable($Name, $Value, 'User')
        $current = $Value
        $script:created += $Name
    }
    [Environment]::SetEnvironmentVariable($Name, $current, 'Process')
}

function New-RandomBytes([int]$Count) {
    $bytes = New-Object byte[] $Count
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    $rng.GetBytes($bytes)
    $rng.Dispose()
    return $bytes
}

function New-DevPassword {
    $chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789'
    $raw = New-RandomBytes 20
    return (($raw | ForEach-Object { $chars[$_ % $chars.Length] }) -join '') + '#9a'
}

$script:created = @()

Set-DevVar 'JWT_SECRET_BASE64' ([Convert]::ToBase64String((New-RandomBytes 32)))
Set-DevVar 'ADMIN_EMAIL' 'admin@tiodaperua.local'
Set-DevVar 'ADMIN_PASSWORD' (New-DevPassword)
Set-DevVar 'GPS_CONSENT_VERSION' 'gps-v1'
Set-DevVar 'TERMS_VERSION' 'termos-v1'
Set-DevVar 'JWT_ACCESS_TOKEN_MINUTES' '120'
Set-DevVar 'DOCS_ENABLED' 'true'
Set-DevVar 'SERVER_PORT' '8090'

if ($script:created.Count -gt 0) {
    Write-Host "Variaveis criadas: $($script:created -join ', ')"
    if ($script:created -contains 'ADMIN_PASSWORD') {
        Write-Host "Login ADMIN: $env:ADMIN_EMAIL / $env:ADMIN_PASSWORD"
    }
} else {
    Write-Host 'Variaveis ja existentes foram carregadas neste terminal.'
}
Write-Host "Swagger: http://localhost:$env:SERVER_PORT/TioDaPerua/api/swagger-ui.html"
