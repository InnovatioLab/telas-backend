<!DOCTYPE html>
<html lang="en">
<head>
    <title>Telas — Scheduled job</title>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <style>
        body { font-family: "Montserrat", sans-serif; font-size: 16px; margin: 0; padding: 24px; background-color: #f7f7f7; color: #111519; }
        strong { color: #075473; font-weight: 600; }
    </style>
</head>
<body>
<p><strong>Scheduled job</strong> <code>${(jobId!"")?html}</code> is about to run.</p>
<p>Time (UTC): ${(startedAt!"")?html}</p>
<p style="font-size: 14px; color: #555;">You receive this message because a cron-style scheduled task is starting on the Telas server.</p>
</body>
</html>
