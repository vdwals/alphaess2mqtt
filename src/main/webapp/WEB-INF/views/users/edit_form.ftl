<@content for="title">Edit User</@content>

<h2>Edit User with id=${user.id}:</h2>

<hr/>

<@form controller="users" id="${user.id}"method="put">
<table>
    <tr>
        <td>Name:</td>
        <td>
            <input type="text" name="name" size="40" value="${user.name}"/>
        </td>
    </tr>
    <tr>
        <td></td>
        <td><input type="submit" value="Update"></td>
    </tr>
</table>



</@form>