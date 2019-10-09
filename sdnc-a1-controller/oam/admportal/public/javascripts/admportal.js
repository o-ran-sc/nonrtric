function uploadFile(form)
{
	if ( form.filename.value.length == 0)
	{
		bootbox.alert('Must select a file.');
		return false;
	}
	else { form.submit(); return; }
}
function isDigit(num)
{
	// false means error
	// true means success
	//var dlNum = obj.value;
	var charAllowed="0123456789";
	var thisChar;
    var cnt = 0;
    var blankCnt = 0;
    if ( num.length > 0 )
    {
        for (var i = 0; i < num.length; i++)
        {
            thisChar = num.substring(i, i+1);
            if ( charAllowed.indexOf(thisChar) != -1 ) cnt++;
            if ( num.substring(i, i + 1) == " " ) blankCnt++;
        }
        if ( cnt != num.length ){
        	return false;
        }
        else if (blankCnt == num.length) {
        	return false;
        }
        return true;
     }
	 else
     	return false;
}
function isFloat(num)
{
	// false means error
	// true means success
	//var dlNum = obj.value;
	var charAllowed=".-0123456789";
	var thisChar;
    var cnt = 0;
    var blankCnt = 0;
    if ( num.length > 0 )
    {
        for (var i = 0; i < num.length; i++)
        {
            thisChar = num.substring(i, i+1);
            if ( charAllowed.indexOf(thisChar) != -1 ) cnt++;
            if ( num.substring(i, i + 1) == " " ) blankCnt++;
        }
        if ( cnt != num.length ){
        	return false;
        }
        else if (blankCnt == num.length) {
        	return false;
        }
        return true;
     }
	 else
     	return false;
}
function isblank(s)
{
    // true means all blank
    // flase means not blank
    for(var i=0; i<s.length; i++) {
        var c = s.charAt(i);
        if ( (c != ' ') && (c != '\n') && (c != '\t') ) return false;
    }
    return true;
}

function padLeft(nr, n, str){
    return Array(n-String(nr).length+1).join(str||'0')+nr;
}
