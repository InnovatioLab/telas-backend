<#import "email_monitoring_layout.ftlh" as mon>
<#assign isDown = (statusLabel!"" == "deactivated") />
<#assign emphasisVariant = "" />
<#if isDown><#assign emphasisVariant = "warning" /></#if>
<@mon.monitoringEmail title="Box status updated" emphasis=emphasisVariant>
    <tr>
        <td style="padding: 14px 0; border-bottom: 1px solid #eceff1;">
            <p class="field-label">Box IP</p>
            <p class="field-value">${ip!""}</p>
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
            <p class="field-label">Linked monitor address(es)</p>
            <p class="field-value">${monitorAddresses!""}</p>
        </td>
    </tr>
    <tr>
        <td style="padding: 14px 0; border-bottom: 1px solid #eceff1;">
            <p class="field-label">Incident</p>
            <p class="field-value"><#if (incidentType!"")?has_content>${incidentType!""}<#if (severity!"")?has_content> (${severity!""})</#if><#else>—</#if></p>
        </td>
    </tr>
    <tr>
        <td style="padding: 14px 0; border-bottom: 1px solid #eceff1;">
            <p class="field-label">Notification time</p>
            <p class="field-value">${notifiedAt!""}</p>
        </td>
    </tr>
    <#if (downtime!"")?has_content>
    <tr>
        <td style="padding: 14px 0;">
            <p class="field-label">Downtime</p>
            <p class="field-value">${downtime}</p>
        </td>
    </tr>
    </#if>
</@mon.monitoringEmail>
