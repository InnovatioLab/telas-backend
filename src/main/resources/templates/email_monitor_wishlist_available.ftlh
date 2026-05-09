<!DOCTYPE html>
<html lang="en">
<head>
    <title>Telas — Screen available</title>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <link rel="preconnect" href="https://fonts.googleapis.com"/>
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin/>
    <link href="https://fonts.googleapis.com/css2?family=Montserrat:wght@400;500;600;700&display=swap" rel="stylesheet"/>
    <style>
        body { font-family: "Montserrat", sans-serif; font-size: 16px; margin: 0; padding: 0; background-color: #f7f7f7; color: #111519; }
        td { font-size: 16px; line-height: 28px; }
        strong { color: #075473; font-weight: 600; }
        .container { width: 100%; margin: 0 auto; padding: 30px 0; }
        .content { max-width: 640px; border-radius: 16px 16px 0 0; background-color: #ffffff; margin: 0 auto; padding: 32px 40px 28px 40px; box-shadow: 0 4px 4px rgba(0, 0, 0, 0.15); }
        .footer { max-width: 640px; line-height: 28px; margin: 0 auto; padding: 24px 40px 32px 40px; font-size: 14px; background-color: #ffffff; border-radius: 0 0 16px 16px; box-shadow: 0 4px 4px rgba(0, 0, 0, 0.15); color: #5a5f66; }
        h3 { color: #075473; margin: 0 0 16px 0; font-weight: 600; font-size: 22px; }
        .muted { font-size: 14px; color: #5a5f66; margin-top: 24px; }
        .locations { margin: 12px 0 0 0; padding: 12px 16px; background: #f0f4f6; border-radius: 8px; font-size: 14px; color: #333; }
    </style>
</head>
<body>
<div class="container">
    <div class="content">
        <table role="presentation" style="width: 100%; border-collapse: collapse;">
            <tr>
                <td style="padding-bottom: 16px"><h3>A screen on your wish list is available</h3></td>
            </tr>
            <tr>
                <td style="padding-bottom: 16px">
                    Hi <strong>${name!""}</strong>, one or more screens you saved now have availability. You can open your wish list and add them to your cart.
                </td>
            </tr>
            <#if (monitorsAddress!"")?has_content>
            <tr>
                <td style="padding-bottom: 16px">
                    <p style="margin: 0 0 8px 0; font-weight: 600; color: #333;">Location(s):</p>
                    <div class="locations">${monitorsAddress}</div>
                </td>
            </tr>
            </#if>
            <tr>
                <td style="padding-top: 8px;">
                    <#if link?has_content>
                    <a href="${link}" style="color: #075473; font-weight: 600;">Open wish list</a>
                    </#if>
                </td>
            </tr>
        </table>
    </div>
    <div class="footer">
        <p style="margin: 0">Telas</p>
        <p class="muted" style="margin: 12px 0 0 0;">Need help? Contact us at support@telas-ads.com</p>
    </div>
</div>
</body>
</html>
