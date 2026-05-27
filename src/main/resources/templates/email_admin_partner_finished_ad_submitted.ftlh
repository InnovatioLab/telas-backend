<!DOCTYPE html>
<html lang="en">
<head>
    <title>Telas — Partner Finished Ad submitted</title>
    <meta charset="UTF-8"/>
    <style>body{font-family:Montserrat,sans-serif;color:#111519;}</style>
</head>
<body>
<p>Hi,</p>
<p><strong>${partnerName!""}</strong> submitted a <strong>Finished Ad</strong> for screen <strong>${monitorLabel!""}</strong>.</p>
<#if instructions?has_content>
<p><strong>Instructions:</strong> ${instructions}</p>
</#if>
<p><a href="${link!""}">Open partner ad requests</a></p>
</body>
</html>
