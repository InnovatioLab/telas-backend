<#assign cName = (clientName!'Client')>
<#assign aName = (adName!'Ad')>
<#assign actor = (adminName!'')>
<#assign linkHref = (link!'')>

<html>
  <body style="font-family: Arial, sans-serif; color: #1f2937;">
    <h2 style="margin: 0 0 12px 0;">Ad resent to customer for validation</h2>
    <p style="margin: 0 0 8px 0;">
      Customer <strong>${cName}</strong> — ad file <strong>${aName}</strong> was updated and is pending client approval again.
    </p>
    <#if actor?has_content>
      <p style="margin: 0 0 8px 0;">Uploaded / published by: <strong>${actor}</strong></p>
    </#if>
    <#if linkHref?has_content>
      <p style="margin: 12px 0 0 0;">
        <a href="${linkHref}" target="_blank" rel="noopener noreferrer">Open Ads management</a>
      </p>
    </#if>
  </body>
</html>
