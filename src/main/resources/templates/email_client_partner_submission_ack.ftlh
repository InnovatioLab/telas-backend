<!DOCTYPE html>
<html lang="en">
<head>
    <title>Telas — Submission received</title>
    <meta charset="UTF-8"/>
    <style>body{font-family:Montserrat,sans-serif;color:#111519;}</style>
</head>
<body>
<p>Hi <strong>${name!""}</strong>,</p>
<#if submissionKind?? && submissionKind == "create_ad">
<p>We received your <strong>Create Ad</strong> request for screen <strong>${monitorLabel!""}</strong>.</p>
<p>Our team will use your reference files and instructions to prepare the ad for this screen.</p>
<#else>
<p>Thank you for uploading your Ad. We'll notify you once it has been uploaded to the screen.</p>
</#if>
<p><a href="${link!""}">Open ${linkLabel!"My screens"}</a></p>
</body>
</html>
