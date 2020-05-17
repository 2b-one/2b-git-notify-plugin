
function doPost(url ='', data = null) {
    return fetch(url, {
        method: 'POST',
        body: data
    })
}

document.addEventListener('DOMContentLoaded', () => {
    let url = window.location.href.split('?')[0]

    document.getElementById('settings-form').addEventListener('submit', function(event) {
        let form = new FormData(document.getElementById('settings-form'))
        form.append('host', document.getElementById('text-input').value)

        let button = document.getElementById('submit-button')

        if (!button.isBusy()) {
            button.busy();

            doPost(url, form)
                .then(() => {
                    button.idle()
                })
        }
    })
})
