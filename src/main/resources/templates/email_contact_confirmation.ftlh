<!DOCTYPE html>
<html lang="pt-br">
<head>
    <title>Sistema Raízes Cearenses</title>
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

        h1, h3 {
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
                <td style="padding-bottom: 30px; padding-top: 20px;"><h1></h1></td>
            </tr>
            <tr>
                <td style="padding-bottom: 10px;">Hello!<strong style="font-weight: 500;"> ${name}</strong></td>
            </tr>
            <tr>
                <td>Welcome to X!</td>
            </tr>
            <tr>
                <td style="padding-bottom: 30px;">
                    To complete your registration and start exploring our features, enter the code <br> below on
                    system validation screen:
                </td>
            </tr>
            <tr>
                <td style="text-align:center;background-color:#ffffff;line-height:50px;color: #00763B;">
                    <h3 style="font-size:26px; font-weight:700;color: #00763B;">Your validation code is:
                        <br> ${verificationCode}
                    </h3>
                </td>
            </tr>
            <tr>
                <td style="padding-top: 30px;">
                    <strong>This code is valid for 15 minutes.</strong> If it expires, you can request a new one in the
                    system.
                </td>
            </tr>
            <tr>
                <td>
                    If you have not registered, please ignore this email.
                </td>
            </tr>
            <tr>
                <td>
                    If you have any questions, our team is available to help!
                </td>
            </tr>
        </table>
    </div>
    <div class="footer">
        <p>Graciously,<br></p>
        <p>Contact: <a href="mailto:raizes.cearenses@ematerce.ce.gov.br" style="color: #131518;">test@gmail.com</a>
        </p>
    </div>
</div>
</body>
</html>