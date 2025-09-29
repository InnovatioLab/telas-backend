<!DOCTYPE html>
<html lang="en">
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
            font-family: "Montserrat", sans-serif;
            font-size: 25px;
            margin: 0;
            padding: 0;
            background-color: #ffffff;
            height: 100vh;
            width: 100vw;
        }

        td {
            font-size: 18px;
            line-height: 42px;
        }

        h1,
        h3 {
            color: #075473;
        }

        h1 {
            font-weight: 600;
            font-size: 32px;
        }

        strong {
            color: #075473;
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
            color: #111519;
            box-shadow: 0px 4px 4px 0px #00000040;
        }

        .footer {
            color: #111519;
            max-width: 1300px;
            line-height: 35px;
            margin: 0 auto;
            text-align: left;
            font-size: 15px;
            background-color: #ffffff;
            padding: 40px 0px 20px 80px;
            border-radius: 0px 0px 25px 25px;
            box-shadow: 0px 4px 4px 0px #00000040;
        }

        h1,
        h3,
        p {
            margin: 0;
        }
    </style>
</head>
<body style="font-family: 'Montserrat', sans-serif">
<div class="container">
    <div class="content">
        <table
                role="presentation"
                style="width: 100%; border-collapse: collapse"
        >
            <tr class="header">
                <td style="padding-bottom: 10px">
                    <h3 style="color: #111519; font-weight: 500; padding-top: 20px">
                        Hi<strong> ${name}</strong>
                    </h3>
                </td>
            </tr>
            <tr>
                <td style="padding-bottom: 10px">
                    We hope you’ve enjoyed your first month with <strong>Telas</strong>. Your ads have already been seen
                    by
                    customers right where buying decisions happen—inside their favorite stores.
                </td>
            </tr>
            <tr>
                <td style="padding-bottom: 10px">
                    Here’s why continuing with a <strong>subscription plan</strong> makes sense:
                </td>
            </tr>
            <tr>
                <td><strong>1. Consistent Visibility: </strong>Repetition builds memory. The longer your ads run, the
                    more
                    your brand stays top-of-mind.
                </td>
            </tr>
            <tr>
                <td><strong>2. Local Reach: </strong>Telas put your business directly in front of your community, not
                    lost
                    in the noise of social media.
                </td>
            </tr>
            <tr>
                <td style="padding-bottom: 10px"><strong>3. Affordable Growth: </strong>With our flexible subscription
                    plans, you can maintain exposure
                    month after month at predictable costs.
                </td>
            </tr>
            <tr>
                <td style="padding-bottom: 10px">Your current one-month service ends on <strong>${endDate}</strong>.
                    Don’t miss out on
                    the brand awareness
                    you’ve already started building.
                </td>
            </tr>
            <tr>
                <td>
                    <strong><a
                                href="${link}"
                                style="color: #075473; text-decoration: none"
                        >Renew Now</a
                        ></strong>
                    to keep your business in front of customers.
                </td>
            </tr>
            <tr>
                <td>If you have any questions, our team is available to help!</td>
            </tr>
            <tr>
                <td>
                    Thank you for choosing Telas. We look forward to continue serving
                    you!
                </td>
            </tr>
        </table>
    </div>
    <div class="footer" style="padding-top: 10px">
        <p style="margin: 0">Best regards,</p>
        <p style="margin: 0"><strong>The Telas Team</strong></p>
        <p style="margin: 5px 0 0">
            Contact us at
            <a href="mailto:support@telas-ads.com" style="color: #111519"
            >support@telas-ads.com</a
            >
        </p>
    </div>
</div>
</body>
</html>
