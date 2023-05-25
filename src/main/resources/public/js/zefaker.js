//list of all columns
let columns = []

function addColumn(column) {
    columns.push(column)
}

function editColumn(column) {
    let columnIdex = column.column_index
    let newList = []
    for (let i = 0; i < columns.length; i++) {
        const element = columns[i];
        if(element.columnIdex==columnIdex){
            newList.push(column)
        }else{
            newList.push(element)
        }
    }
    columns = newList
}

function exportData(format="sql") {
    //create a groovy file from columns data
    let groovyStringFormat = `import java.util.concurrent.atomic.AtomicInteger 
    \nautoIncrementID = new AtomicInteger(0)`
    let declarations = ``;
    let generator = ``;

    for (let index = 0; index < columns.length; index++) {
        const column = columns[index];
        declarations += formatDeclaration(column,index)+"\n";
        if(index==0){
            generator += formatAsGenerator(column)+"\n";
        }else{
            generator += ",\n"+formatAsGenerator(column)+"";
        }
    }

    groovyStringFormat+=declarations+"\n"+generator;
    let formData = {
        fileName: "data." + format,
        code: groovyStringFormat,
        format: format,
        sheet: document.getElementById('sheet').value,
        sqlquote: document.getElementById('sqlquote').value,
        rows: document.getElementById('numberOfRows').value,
        table: document.getElementById('table').value,
      }

    //   document.getElementById("groovy-output").innerHTML = groovyStringFormat
    // var textArea = document.getElementById('groovy-output');
    // var editor = CodeMirror.fromTextArea(textArea);
    // editor.getDoc().setValue(groovyStringFormat);
    const url = "/generate";

      axios({
            method: 'post',
            type: 'json',
            url: url,
            data: JSON.stringify(formData),
            responseType: 'blob',
            headers: {
              'Content-Type': 'application/json',
              'Accept': 'application/octet-stream;charset=utf-8,application/json'
            }
        })
        .then(function checkStatus(response) {
            if (response.status != 200) {
                let error = new Error(response.statusText);
                error.response = response;
                throw error;
            }
            return response;    
        })
        .then(response => {
          
          saveAs(response.data, formData.fileName);
          progressModal.hide()
          alert("Saved Successfully!")
        })
        .catch(err => {
            modalBody.innerHTML = '<div class="error hidden">Failed to generate file: ' + err + '</div>'
          // progressModal.hide()
          // General conenction error
          if ('response' in err) {
              const response = err.response;
              if (response.status === 409) {
                  return Promise.resolve({
                      message: 'File uploaded already exists on the server',
                      fileAlreadyExists: true
                    });
                }
            }
            alert("Saved Successfully!")
        });
    }
    
    function formatDeclaration(params, index) {
        let name = "";
    if(params.column_data_type=="First Name"){
        name = "name";
    }else if (params.column_data_type=="Last Name"){
        name="";
    }
    name = params.column_name
    var columnString = 
    `${params.column_name} = column(index=${index}, name="${name}")`;
    return columnString
}

function formatAsGenerator(params) {
    var dataType = ""
    if(params.column_data_type=="First Name"){
        dataType = "faker.name().firstName()";
    }else if(params.column_data_type=="Last Name"){
        dataType = "faker.name().lastName()";
    }else if(params.column_data_type=="Auto Increment Value"){
        dataType = "faker.autoIncrementID.incrementAndGet()";
    }else if(params.column_data_type=="number_range"){
        dataType = `faker.number().numberBetween(${params.column_properties.split(":")[0]}, ${params.column_properties.split(":")[1]})`;
    }else{
        let csvs = ``;
        let d1 = params.column_properties
        let d2 = d1.split(",")
        for(var k=0;k<d2.length;k++){
            if(k==0){
                csvs += `"${d2[k]}"`;
            }else{
                csvs += `,"${d2[k]}"`;
            }
        }
        dataType = `faker -> faker.options().option(${csvs})`;
    }
    let generatedScript = 
    `(${params.column_name}):{faker -> ${dataType}}`

    return generatedScript
}

function createNewColumn() {
    let column = {"column_index":1,"column_name":"","column_properties":"","column_data_type":""};
    column.column_name = document.getElementById("zf-column-name").value
    let dataType = document.getElementById("zf-data-type").value
    column.column_data_type = dataType
    if(dataType=="number_range"){
        column.column_properties = `${document.getElementById("zefaker-range-from").value}:${document.getElementById("zefaker-range-to").value}`
    }else if(dataType=="string_options"){
        column.column_properties = document.getElementById("zefaker-string-option").value
    }else{
        column.column_properties = "auto generated"
    }
    column.column_index = columns.length+1
    columns.push(column)
    addColumnToUI(column);
    $("#exampleModal3").modal("hide")
}

function addColumnToUI(column) {
    let cloumnUI= document.getElementById("columns-ui")
    let columnHTML = document.createElement("div")
    columnHTML.className = "col-sm-3"
    columnHTML.id = "c-"+columns.length+1
    columnHTML.innerHTML = `<div class="card">
    <div class="card-body">
        <button class="btn" onclick="deleteColumn('${columnHTML.id}')" style="position: absolute;top: 0;right: 0;">&times;</button>
        <b>${column.column_name}</b>
        <hr class="m-0 p-0">
        <small>Type</small> <br>
        <span class="text-danger">[${column.column_data_type}]</span> <br>
        <small>value</small>
        <input type="text" disabled class="form-control" value="${column.column_properties}">
        </div>
    </div>`
    cloumnUI.appendChild(columnHTML);
}


function dataTypeChanged(event) {
    var activeDataType = event.target.value
    hide("zefaker-range")
    hide("zefaker-string-options")
    if(activeDataType=="number_range"){
        show("zefaker-range")
    }else if(activeDataType=="string_options"){
        show("zefaker-string-options")
    }
}




function show(id) {
    document.getElementById(id).style.display = "block"
}

function hide(id) {
    document.getElementById(id).style.display = "none"
}

function deleteColumn(columnID) {
    document.getElementById(columnID).remove()
    let column_index = columnID.split("-")[1]
    let newColumns = []
    for (let index = 0; index < columns.length; index++) {
        const column = columns[index];
        if(index==column_index){}
        else{
            newColumns.push(column)
        }
    }
    columns = newColumns
}