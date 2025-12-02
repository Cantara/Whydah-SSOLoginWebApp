<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1"/>
    <meta name="detectify-verification" content="3696809a6c4e05d6f4385ce717fea51c"/>
    <meta http-equiv="x-content-type-options" content="nosniff">
    <meta http-equiv="x-xss-protection" content="1; mode=block">
    <link rel="shortcut icon" href="images/favicon.ico" type="image/x-icon"/>
    <link rel="icon" href="images/favicon.ico" type="image/x-icon"/>
    <title>Temporary issue</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <style>
        body { background: linear-gradient(to bottom, #f8fafc, #e2e8f0); }
    </style>
</head>

<body class="min-h-screen flex items-center justify-center px-4">
<div class="max-w-xl w-full bg-white rounded-2xl shadow-xl p-10 text-center space-y-8">

    <!-- Orange warning icon -->
    <div class="mx-auto w-28 h-28 bg-orange-100 rounded-full flex items-center justify-center">
        <svg class="w-14 h-14 text-orange-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-2.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"/>
        </svg>
    </div>

    <div class="space-y-5">
        <h1 class="text-3xl font-bold text-gray-900">Something went wrong</h1>

        <#-- Show the resolved message (either translated STS or original English) -->
        <#if errorMessage?? && (errorMessage?trim)?has_content>
            <p class="text-lg text-gray-700 leading-relaxed">${errorMessage?html}</p>
        <#else>
            <p class="text-lg text-gray-700 leading-relaxed">
                The service is temporarily unavailable due to a technical issue.
            </p>
        </#if>

        <p class="text-gray-600">
            Our team has been notified automatically and we’re already working on it.<br>
            It usually only takes a few minutes.
        </p>

        <p class="text-sm text-gray-500 font-medium">
            Sorry for the inconvenience – thank you for your patience!
        </p>
    </div>

    <div class="pt-6">
        <a href="javascript:location.reload();"
           class="inline-block px-8 py-3 bg-orange-600 text-white font-semibold rounded-lg hover:bg-orange-700 transition shadow-md">
            Try again now
        </a>
    </div>

</div>
</body>
</html>