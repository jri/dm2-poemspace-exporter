<%
	String json = (String) session.getAttribute("json");
%>

<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
	<title>Poemspace Exporter</title>
</head>
<body>
<pre>
<%= json %>
</pre>
</body>
</html>
