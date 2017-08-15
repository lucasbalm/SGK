var express = require("express");
var bodyParser = require("body-parser");
var fs = require('fs'); 
var im = require('imagemagick');
var cloudinary = require('cloudinary');
var Kairos = require('kairos-api');
var kairosClient = new Kairos('516d4a8b', '0933f0e94aca51e12a7419458247be49');

var app = express();

var url;

cloudinary.config({ 
  cloud_name: 'makssie', 
  api_key: '337679993513188', 
  api_secret: 'ItvczwBrlix4mM1c6jGcDMeWTQM' 
});

//Here we are configuring express to use body-parser as middle-ware.
app.use(bodyParser.urlencoded({limit: '50mb', extended: true}));
app.use(bodyParser.json({limit: '50mb'}));

var count = 1;

app.post('/addOwner', function(request, response){
    console.log(request.body.image);
    console.log(request.body.userName);  

    var bitmap = new Buffer(request.body.image, 'base64');
    // write buffer to file

    fs.writeFileSync('ownerImage.jpg', bitmap);    
    console.log('******** File created from base64 encoded string ********');    
    response.json({
      message : "teste feito"
     })

    // im.resize({
    //   srcPath: 'ownerImage.jpg',
    //   dstPath: 'ownerImage.jpg',
    //   width:   800,
    //   height: 800
    // }, function(err, stdout, stderr){
    //   if (err) throw err;
    //   console.log('Resized');
    // });
  
    // upload image to cloudinary
      cloudinary.uploader.upload("ownerImage.jpg", function(result) { 
        console.log(result);
    
        url = result.url;

        kairos_recog(url,request.body.userName, function() {
          console.log("success");  //Ou response.send
        });
      }); 

    });   //Comentar até aqui


function kairos_recog(url,userName, callback) {

  var params = {
    image: url,
    subject_id: userName,
    gallery_name: 'smartdoor',
    selector: 'SETPOSE'
  };

  kairosClient.enroll(params)   
    .then(function(result) {
      console.log("Inside then");
      console.log(result.status);      
      console.log(result.body['images'][0]);
      callback();
    })
    // err -> array: jsonschema validate errors 
    //        or throw Error 
    .catch(function(err) {
      console.log("Inside err");
      console.log(err);
    });

}



app.listen(3000,function(){
  console.log("app.js started on http://localhost:3000");
})


