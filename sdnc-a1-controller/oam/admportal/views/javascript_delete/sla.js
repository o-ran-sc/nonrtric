<script>
function uploadDG(form)
{
	if ( form.filename.value.length == 0)
	{
		alert('Must select a file.');
		return false;
	}
	else { form.submit(); return; }
}
</script>
