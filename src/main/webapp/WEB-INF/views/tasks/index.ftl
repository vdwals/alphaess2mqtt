[
<#if tasks?size == 0>
    <#else>
    <@render partial="task" collection=tasks spacer="comma"/>
</#if>
]




