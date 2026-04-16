<!DOCTYPE html>
<html lang="en">
<head>
    <title>Telas — New purchase</title>
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
        .content { max-width: 1300px; border-radius: 25px 25px 0 0; background-color: #ffffff; margin: 0 auto; padding: 30px 0 0 80px; box-shadow: 0 4px 4px 0 #00000040; }
        .footer { max-width: 1300px; line-height: 28px; margin: 0 auto; padding: 24px 0 20px 80px; font-size: 14px; background-color: #ffffff; border-radius: 0 0 25px 25px; box-shadow: 0 4px 4px 0 #00000040; }
        h3 { color: #075473; margin: 0 0 12px 0; font-weight: 600; }
        .section-title { font-weight: 600; padding-top: 16px; padding-bottom: 8px; color: #111519; }
    </style>
</head>
<body>
<div class="container">
    <div class="content">
        <table role="presentation" style="width: 100%; border-collapse: collapse; padding-right: 80px;">
            <tr>
                <td style="padding-bottom: 16px">
                    <h3>New purchase completed</h3>
                </td>
            </tr>
            <tr>
                <td style="padding-bottom: 8px"><strong>Customer:</strong> ${buyerName!""}</td>
            </tr>
            <tr>
                <td style="padding-bottom: 8px"><strong>Subscription ID:</strong> ${subscriptionId!""}</td>
            </tr>
            <tr>
                <td class="section-title">Locations — blocks purchased &amp; full address</td>
            </tr>
            <tr>
                <td style="padding-bottom: 16px">${monitorsDetailHtml?no_esc}</td>
            </tr>
            <tr>
                <td class="section-title">Uploaded files (ad attachments)</td>
            </tr>
            <tr>
                <td style="padding-bottom: 16px">${attachmentListHtml?no_esc}</td>
            </tr>
            <tr>
                <td class="section-title">Advertising period</td>
            </tr>
            <tr>
                <td style="padding-bottom: 24px">${veiculationSummary!""}</td>
            </tr>
        </table>
    </div>
    <div class="footer">
        <p style="margin: 0">Telas — internal notification</p>
    </div>
</div>
</body>
</html>
