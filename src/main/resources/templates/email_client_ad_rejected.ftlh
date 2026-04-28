<#assign clientName = (name!'Customer')>
<#assign adName = (adName!'Ad')>

<html>
  <body style="font-family: Arial, sans-serif; color: #1f2937;">
    <h2 style="margin: 0 0 12px 0;">Ad rejected by customer</h2>
    <p style="margin: 0 0 8px 0;">
      <strong>${clientName}</strong> rejected the ad <strong>${adName}</strong>.
    </p>
    <#if justification?? && (justification?trim?length > 0)>
      <p style="margin: 0 0 8px 0;"><strong>Justification:</strong> ${justification}</p>
    </#if>
    <#if description?? && (description?trim?length > 0)>
      <p style="margin: 0 0 8px 0;"><strong>Description:</strong> ${description}</p>
    </#if>
    <#if link?? && (link?trim?length > 0)>
      <p style="margin: 12px 0 0 0;">
        <a href="${link}" target="_blank" rel="noopener noreferrer">Open ad request / client details</a>
      </p>
    </#if>
  </body>
</html>
