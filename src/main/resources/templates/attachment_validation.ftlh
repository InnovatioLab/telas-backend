<!DOCTYPE html>
<html lang="pt-br">
<head>
    <title>Telas</title>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <meta http-equiv="X-UA-Compatible" content="ie=edge"/>
    <link rel="preconnect" href="https://fonts.googleapis.com"/>
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin/>
    <link
            href="https://fonts.googleapis.com/css2?family=Montserrat:wght@400;500;600;700&display=swap"
            rel="stylesheet"
    />
    <style>
        body {
            font-family: 'Montserrat', sans-serif;
            font-size: 25px;
            margin: 0;
            padding: 0;
            background-color: #ffffff;
            height: 100vh;
            width: 100vw;
        }

        td {
            font-size: 22px;
            line-height: 42px;
        }

        h1, h3, .label, .container-link {
            color: #00763B;
        }

        h1 {
            font-weight: 600;
            font-size: 32px;
        }

        strong {
            color: #00763B;
            font-weight: 600;
        }

        .container {
            width: 100%;
            height: 95%;
            margin: 0 auto;
            padding: 30px 0;
            background-color: #f7f7f7;
            border-radius: 10px;
        }

        .header {
            text-align: center;
            background-color: #ffffff;
            padding: 20px;
            margin-bottom: 2rem;
            border-radius: 10px 10px 0 0;
        }

        .content {
            max-width: 1300px;
            border-radius: 25px 25px 0px 0px;
            background-color: #ffffff;
            margin: 0 auto;
            padding-left: 80px;
            font-size: 16px;
            color: #131518;
            box-shadow: 0px 4px 4px 0px #00000040;
        }

        .container-link {
            text-align: center;
            margin: 0 auto;
            width: 100%;
            padding-top: 30px;
        }

        .footer {
            color: #131518;
            max-width: 1300px;
            line-height: 35px;
            margin: 0 auto;
            text-align: left;
            font-size: 18px;
            background-color: #ffffff;
            padding: 40px 0px 20px 80px;
            border-radius: 0px 0px 25px 25px;
            box-shadow: 0px 4px 4px 0px #00000040;
        }

        h1, h3, p {
            margin: 0;
        }

    </style>
</head>
<body style="font-family: 'Montserrat', sans-serif;">
<div class="container">
    <div class="content">
        <table role="presentation" style="width: 100%; border-collapse: collapse;">
            <tr class="header">
                <td style="padding-bottom: 30px; padding-top: 20px;"><h1>Telas</h1></td>
            </tr>
            <tr>
                <td style="padding-bottom: 20px;">
                    Hello, ${recipient}
                </td>
            </tr>
            <tr>
                <td style="padding-bottom: 30px;">
                    Your attachment has been validated!
                </td>
            </tr>
            <tr>
                <td style="padding-bottom: 10px;">
                    <strong>Attachment:</strong> ${attachmentName}
                </td>
            </tr>
            <tr>
                <td style="padding-bottom: 10px;">
                    <strong>Status:</strong> ${status}
                </td>
            </tr>
            <#if justification?has_content>
                <tr>
                    <td style="padding-bottom: 10px;">
                        <strong>Justification:</strong> ${justification}
                    </td>
                </tr>
            </#if>
            <#if description?has_content>
                <tr>
                    <td style="padding-bottom: 10px;">
                        <strong>Description:</strong> ${description}
                    </td>
                </tr>
            </#if>
            <tr>
                <td class="container-link">
                    View details<strong>
                        <br>
                        <a href="${link}" target="_blank"
                           style="color: #00763B; text-decoration: none;">Link</a></strong>
                </td>
            </tr>
        </table>
    </div>
    <div class="footer" style="margin-top: 40px; border-top: 1px solid #e0e0e0; padding-top: 20px;">
        <p style="margin:0;">Best regards,</p>
        <p style="margin:0;"><strong>The Tela's Team</strong></p>
        <p style="margin:5px 0 0;">Contact us at
            <a href="mailto:support@telas.com" style="color: #131518;">support@telas.com</a>
        </p>
    </div>
</div>
</body>
</html>