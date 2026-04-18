<#import "email_monitoring_layout.ftlh" as mon>
<@mon.monitoringEmail title="Host reboot detected" emphasis="warning">
    <tr>
        <td style="padding: 14px 0; border-bottom: 1px solid #eceff1;">
            <p class="field-label">Box IP</p>
            <p class="field-value">${boxIp!""}</p>
        </td>
    </tr>
    <tr>
        <td style="padding: 14px 0; border-bottom: 1px solid #eceff1;">
            <p class="field-label">Incident</p>
            <p class="field-value">${incidentType!""}<#if (severity!"")?has_content> (severity ${severity!""})</#if></p>
        </td>
    </tr>
    <tr>
        <td style="padding: 14px 0; border-bottom: 1px solid #eceff1;">
            <p class="field-label">Uptime drop (seconds)</p>
            <p class="field-value">${uptimeDropSeconds!""}</p>
        </td>
    </tr>
    <tr>
        <td style="padding: 14px 0;">
            <p class="field-label">Notification time</p>
            <p class="field-value">${notifiedAt!""}</p>
        </td>
    </tr>
</@mon.monitoringEmail>
