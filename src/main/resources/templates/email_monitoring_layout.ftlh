<#macro monitoringEmail title emphasis="">
<!DOCTYPE html>
<html lang="en">
<head>
    <title>Telas</title>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <meta http-equiv="X-UA-Compatible" content="ie=edge"/>
    <link rel="preconnect" href="https://fonts.googleapis.com"/>
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin/>
    <link href="https://fonts.googleapis.com/css2?family=Montserrat:wght@400;500;600;700&display=swap" rel="stylesheet"/>
    <style>
        body {
            font-family: 'Montserrat', sans-serif;
            font-size: 16px;
            margin: 0;
            padding: 0;
            background-color: #ffffff;
        }
        td {
            font-size: 16px;
            line-height: 1.6;
            color: #111519;
        }
        h1 {
            color: #075473;
            font-weight: 600;
            font-size: 22px;
            margin: 0;
        }
        .brand-wordmark {
            font-size: 22px;
            font-weight: 700;
            color: #075473;
            letter-spacing: 0.02em;
            margin: 0;
        }
        .brand-tagline {
            font-size: 12px;
            color: #5a5f66;
            text-transform: uppercase;
            letter-spacing: 0.1em;
            margin: 6px 0 0 0;
        }
        .container {
            width: 100%;
            margin: 0 auto;
            padding: 28px 16px;
            background-color: #f7f7f7;
        }
        .content {
            max-width: 640px;
            border-radius: 16px 16px 0 0;
            background-color: #ffffff;
            margin: 0 auto;
            padding: 32px 40px 28px 40px;
            box-shadow: 0 4px 4px rgba(0, 0, 0, 0.15);
        }
        .footer {
            color: #111519;
            max-width: 640px;
            line-height: 1.7;
            margin: 0 auto;
            font-size: 15px;
            background-color: #ffffff;
            padding: 28px 40px 36px 40px;
            border-radius: 0 0 16px 16px;
            box-shadow: 0 4px 4px rgba(0, 0, 0, 0.15);
        }
        .field-label {
            font-size: 13px;
            color: #5a5f66;
            font-weight: 500;
            margin: 0 0 4px 0;
        }
        .field-value {
            font-size: 16px;
            color: #111519;
            margin: 0;
        }
        .emphasis-banner {
            border-radius: 8px;
            padding: 14px 16px;
            margin-bottom: 20px;
            font-size: 15px;
            line-height: 1.5;
        }
        .emphasis-warning {
            background-color: #fff3e0;
            border-left: 4px solid #e65100;
            color: #4a2800;
        }
        .emphasis-critical {
            background-color: #ffebee;
            border-left: 4px solid #c62828;
            color: #4a1015;
        }
    </style>
</head>
<body style="font-family: 'Montserrat', Arial, sans-serif;">
<div class="container">
    <div class="content">
        <table role="presentation" style="width: 100%; border-collapse: collapse;">
            <tr>
                <td style="padding-bottom: 24px;">
                    <p class="brand-wordmark">Telas</p>
                    <p class="brand-tagline">Monitoring</p>
                </td>
            </tr>
            <tr>
                <td style="padding-bottom: 20px;">
                    <h1>${title}</h1>
                </td>
            </tr>
            <#if emphasis == "warning">
            <tr>
                <td>
                    <div class="emphasis-banner emphasis-warning">
                        <strong>Attention:</strong> connectivity or device status requires your review.
                    </div>
                </td>
            </tr>
            <#elseif emphasis == "critical">
            <tr>
                <td>
                    <div class="emphasis-banner emphasis-critical">
                        <strong>Alert:</strong> a monitoring incident was detected.
                    </div>
                </td>
            </tr>
            </#if>
            <#nested>
        </table>
    </div>
    <div class="footer">
        <p style="margin: 0;">Best regards,</p>
        <p style="margin: 4px 0 0 0;"><strong>The Telas Team</strong></p>
        <p style="margin: 12px 0 0 0;">
            Contact us at
            <a href="mailto:support@telas-ads.com" style="color: #075473; text-decoration: none;">support@telas-ads.com</a>
        </p>
    </div>
</div>
</body>
</html>
</#macro>
