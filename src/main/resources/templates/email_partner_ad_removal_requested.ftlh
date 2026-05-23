<!DOCTYPE html>
<html lang="en">
<head>
    <title>Telas — Partner ad removal request</title>
    <meta charset="UTF-8"/>
    <style>body{font-family:Montserrat,sans-serif;color:#111519;}</style>
</head>
<body>
<p>Hi,</p>
<p><strong>${partnerName!""}</strong> requested removal of ad <strong>${adName!""}</strong>.</p>
<#if screenSummary?has_content><p><strong>Screen:</strong> ${screenSummary}</p></#if>
<#if message?has_content><p><strong>Message:</strong> ${message}</p></#if>
<p><a href="${link!""}">Open ad requests</a></p>
</body>
</html>
