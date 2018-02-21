#Login-AzureRmAccount

$IdentArray = @("0","1","2","3","4","5","6","7","8","9","a","b","c","d","e","f","x")

$Output = ""

Write-Host "Prod Queue Active Message Counts: "

foreach($Ident in $IdentArray) {
	$QueueName = "nsscbuclientupdates."+$Ident

	$QueueAttrib = Get-AzureRmServiceBusQueue -ResourceGroup nssCbuRG -NamespaceName nssCbuProd -QueueName $QueueName

	$Output = $Output + " " + $Ident + ": " + $QueueAttrib.CountDetails.ActiveMessageCount
}

Write-Host $Output