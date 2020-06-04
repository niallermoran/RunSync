# RunSync
This app was built to help extract data from Samsung Health and store it in an environment that would allow the data to be reported on using a BI tool, like [Power BI](https://aka.ms/pbidesktopstore " Power BI").

The app is very basic in that it simply extracts the data and uses a http POST to send the data, in JSON form, to the URL that you define.

If you want to create a URL to post the data to, I would recommend using a Microsoft [Power Automate Flow](https://preview.flow.microsoft.com/en-us/ "Power Automate Flow") with a HTTP trigger. You can then use Power Automate to parse the data and send it to a data source. personally, I have used Table storage in Microsoft Azure and then Power BI to report on the data. The schema for the JSON that is extracted from Samsung Health can be found in the schema.json file.

The app is available in the Google Play store
https://play.google.com/store/apps/details?id=com.niallmoran.runsync
