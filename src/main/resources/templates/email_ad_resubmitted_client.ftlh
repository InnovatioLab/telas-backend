<#assign clientName = (name!'')>
<#assign adName = (adName!'Ad')>
<#assign linkHref = (link!'')>

<html>
  <body style="font-family: Arial, sans-serif; color: #1f2937;">
    <h2 style="margin: 0 0 12px 0;">Your ad was updated</h2>
    <p style="margin: 0 0 8px 0;">
      Hello<#if clientName?has_content> <strong>${clientName}</strong></#if>,
    </p>
    <p style="margin: 0 0 8px 0;">
      We revised your ad <strong>${adName}</strong> after your feedback. Please sign in and validate it again on the Ads tab.
    </p>
    <#if linkHref?has_content>
      <p style="margin: 12px 0 0 0;">
        <a href="${linkHref}" target="_blank" rel="noopener noreferrer">Open My Telas — Ads</a>
      </p>
    </#if>
    <p style="margin: 16px 0 0 0; font-size: 13px; color: #6b7280;">
      If you approve the ad, it can proceed in your campaign flow. If you reject it again, our team will review your notes.
    </p>
  </body>
</html>
