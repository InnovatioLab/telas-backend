<!DOCTYPE html>
<html lang="en">
<head>
    <title>Telas</title>
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
        .content { max-width: 1300px; border-radius: 25px 25px 0 0; background-color: #ffffff; margin: 0 auto; padding-left: 80px; box-shadow: 0 4px 4px 0 #00000040; }
        .footer { max-width: 1300px; line-height: 28px; margin: 0 auto; text-align: left; font-size: 15px; background-color: #ffffff; padding: 40px 0 20px 80px; border-radius: 0 0 25px 25px; box-shadow: 0 4px 4px 0 #00000040; }
        h3 { color: #111519; font-weight: 500; padding-top: 20px; margin: 0; }
    </style>
</head>
<body>
<div class="container">
    <div class="content">
        <table role="presentation" style="width: 100%; border-collapse: collapse; padding-right: 80px;">
            <tr>
                <td style="padding-bottom: 10px">
                    <h3>Hi<strong> ${name?html}</strong></h3>
                </td>
            </tr>
            <tr>
                <td style="padding-bottom: 20px">
                    <strong>Placeholder copy</strong> — Your Telas advertising service ends in <strong>${daysRemaining?html}</strong> day(s).
                    Final copy will be provided later.
                </td>
            </tr>
            <tr>
                <td style="padding-bottom: 10px">
                    Service end date: <strong>${endDate?html}</strong>
                </td>
            </tr>
            <tr>
                <td style="padding-bottom: 20px">
                    <strong><a href="${link?html}" style="color: #075473;">Manage subscription</a></strong>
                </td>
            </tr>
            <tr>
                <td>If you have any questions, our team is available to help!</td>
            </tr>
        </table>
    </div>
    <div class="footer" style="padding-top: 10px">
        <p style="margin: 0">Best regards,</p>
        <p style="margin: 0"><strong>The Telas Team</strong></p>
        <p style="margin: 5px 0 0">
            Contact us at
            <a href="mailto:support@telas-ads.com" style="color: #111519">support@telas-ads.com</a>
        </p>
    </div>
</div>
</body>
</html>
