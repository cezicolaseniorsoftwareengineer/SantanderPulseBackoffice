param(
    [Parameter(Mandatory = $true)]
    [string]$ClientId,

    [Parameter(Mandatory = $true)]
    [string]$ClientSecret,

    [string]$RedirectUri = "http://localhost:8080/api/login/oauth2/code/google",
    [string]$FrontendUrl = "http://localhost:4200",
    [switch]$Persist
)

Write-Host "Configuring Google OAuth environment variables..." -ForegroundColor Cyan

$env:GOOGLE_CLIENT_ID = $ClientId
$env:GOOGLE_CLIENT_SECRET = $ClientSecret
$env:GOOGLE_REDIRECT_URI = $RedirectUri
$env:FRONTEND_URL = $FrontendUrl

if ($Persist.IsPresent) {
    Write-Host "Persisting variables for the current user profile" -ForegroundColor Yellow
    setx GOOGLE_CLIENT_ID $ClientId | Out-Null
    setx GOOGLE_CLIENT_SECRET $ClientSecret | Out-Null
    setx GOOGLE_REDIRECT_URI $RedirectUri | Out-Null
    setx FRONTEND_URL $FrontendUrl | Out-Null
}

Write-Host "GOOGLE_CLIENT_ID     = $ClientId" -ForegroundColor Green
Write-Host "GOOGLE_CLIENT_SECRET = ********" -ForegroundColor Green
Write-Host "GOOGLE_REDIRECT_URI  = $RedirectUri" -ForegroundColor Green
Write-Host "FRONTEND_URL         = $FrontendUrl" -ForegroundColor Green
Write-Host "Variables applied to current PowerShell session." -ForegroundColor Cyan
Write-Host "Run the backend with: mvn spring-boot:run" -ForegroundColor DarkCyan
