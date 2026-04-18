<#import "email_monitoring_layout.ftlh" as mon>
<#assign down = (statusLabel!"" == "deactivated") />
<#assign emphasisVariant = "" />
<#if down><#assign emphasisVariant = "warning" /></#if>
<@mon.monitoringEmail title="Monitor status updated" emphasis=emphasisVariant>
    <tr>
        <td style="padding: 14px 0; border-bottom: 1px solid #eceff1;">
            <p class="field-label">Monitor</p>
            <p class="field-value">${monitorAddress!""}</p>
        </td>
    </tr>
    <tr>
        <td style="padding: 14px 0; border-bottom: 1px solid #eceff1;">
            <p class="field-label">Status</p>
            <p class="field-value">${statusLabel!""}</p>
        </td>
    </tr>
    <tr>
        <td style="padding: 14px 0; border-bottom: 1px solid #eceff1;">
            <p class="field-label">Incident</p>
            <p class="field-value"><#if (incidentType!"")?has_content>${incidentType!""}<#if (severity!"")?has_content> (${severity!""})</#if><#else>—</#if></p>
        </td>
    </tr>
    <tr>
        <td style="padding: 14px 0;">
            <p class="field-label">Notification time</p>
            <p class="field-value">${notifiedAt!""}</p>
        </td>
    </tr>
</@mon.monitoringEmail>
