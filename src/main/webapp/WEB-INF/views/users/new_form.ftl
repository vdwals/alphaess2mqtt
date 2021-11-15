<@content for="title">Create New User</@content>

<h2>Create New User:</h2>

<hr/>

<@form controller="users" method="post">
<table>
    <tr>
        <td>Name:</td>
        <td>
            <input type="text" name="name" size="40" />
        </td>
    </tr>
    <tr>
        <td></td>
        <td><input type="submit" value="Create"></td>
    </tr>
</table>



</@form>