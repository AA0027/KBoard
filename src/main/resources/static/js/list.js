$(function(){
    $("[name='pageRows']").change(function(){
        // alert($(this).val()); // 확인용
        $("[name='frmPageRows']").attr({
            "method": "POST",
            "action": "/board/pageRows",
        }).submit();
    })
})