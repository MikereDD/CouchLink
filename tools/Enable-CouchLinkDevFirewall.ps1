#Requires -RunAsAdministrator
[CmdletBinding()]
param()

$rules = @(
    @{ Name = 'CouchLink Discovery (Dev)'; Protocol = 'UDP'; Port = 45820 },
    @{ Name = 'CouchLink Session (Dev)'; Protocol = 'TCP'; Port = 45821 }
)

foreach ($rule in $rules) {
    $existing = Get-NetFirewallRule -DisplayName $rule.Name -ErrorAction SilentlyContinue
    if ($existing) {
        Set-NetFirewallRule -DisplayName $rule.Name -Enabled True -Profile Private -Action Allow
    }
    else {
        New-NetFirewallRule `
            -DisplayName $rule.Name `
            -Direction Inbound `
            -Action Allow `
            -Protocol $rule.Protocol `
            -LocalPort $rule.Port `
            -Profile Private | Out-Null
    }
    Write-Host "Enabled: $($rule.Name)" -ForegroundColor Green
}
