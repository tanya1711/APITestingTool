import React, { useState, useRef } from 'react';
import Slider from "react-slick";
import './Home.css';
import { createToast } from 'react-simple-toasts';
import 'react-simple-toasts/dist/style.css';
import noRequestsImage from './Assets/no-requests.png';
import somethingWentWrong from './Assets/something-went-wrong.svg';
import jsPDF from 'jspdf';
import 'jspdf-autotable';


const Home = () => {
  const [curl, setCurl] = useState('');
  const [description, setDescription] = useState('');
  const [apiData, setApiData] = useState([]);
  const [error, setError] = useState(null);
  const [currentSlideRequest, setCurrentSlideRequest] = useState(1);
  const [loadingCurl, setLoadingCurl] = useState(false);
  const [loading, setLoading] = useState(false);
  const [charLimit] = useState(500);
  const [toastMessage, setToastMessage] = useState('');
  const [formattedResponse, setFormattedResponse] = useState([]);
  const [isResponseShown, setIsResponseShown] = useState(false);
  const [currentSlide, setCurrentSlide] = useState(0);
  const [isSingleRequest, setIsSingleRequest] = useState(false);
  const requestSliderRef = useRef(null);
  const responseSliderRef = useRef(null);
  const [showFallbackImage, setShowFallbackImage] = useState(false);
  const isButtonDisabled = !curl.trim() || !description.trim() || loadingCurl;


  const handleDownloadPDF = () => {
    const doc = new jsPDF();
    doc.setFontSize(12);
    const margin = 10; // Set a margin
    const lineHeight = 10; // Set line height for text
    const pageHeight = doc.internal.pageSize.getHeight(); // Get page height for content wrapping
    let startY = margin; // Starting position for the first content

    // Function to check if the current Y position exceeds the page height
    const checkPageHeight = (linesToAdd) => {
      return startY + (lineHeight * linesToAdd) > (pageHeight - margin);
    };

    // Function to add a new page
    const addPage = () => {
      doc.addPage();
      startY = margin; // Reset starting Y position
    };

    // Add cURL section
    doc.setFont("helvetica", "bold");
    doc.text("cURL:", margin, startY);
    doc.setFont("helvetica", "normal");
    const curlLines = doc.splitTextToSize(curl, doc.internal.pageSize.getWidth() - margin * 2);
    if (checkPageHeight(curlLines.length + 1)) addPage();
    doc.text(curlLines, margin, startY);
    startY += lineHeight * (curlLines.length + 1); // Move Y position down

    // Add Description section
    doc.setFont("helvetica", "bold");
    doc.text("Description:", margin, startY);
    doc.setFont("helvetica", "normal");
    const descriptionLines = doc.splitTextToSize(description, doc.internal.pageSize.getWidth() - margin * 2);
    if (checkPageHeight(descriptionLines.length + 1)) addPage();
    doc.text(descriptionLines, margin, startY);
    startY += lineHeight * (descriptionLines.length + 1); // Move Y position down

    // Add Test Cases and Responses
    doc.setFont("helvetica", "bold");
    doc.text("Test Cases and Responses:", margin, startY);
    startY += lineHeight; // Move down for the first test case

    apiData.forEach((item) => {
      // Check for corresponding response
      const response = formattedResponse.find(res => res.tcId === item.id);

      // Add Test Case and Request Body
      const requestYPosition = startY;
      const requestTitle = `Test Case ${item.id}: ${item.title}`;
      const requestLines = doc.splitTextToSize(item.description, (doc.internal.pageSize.getWidth() / 2) - margin * 2);

      // Draw request title and body
      doc.setFont("helvetica", "bold");
      doc.text(requestTitle, margin, requestYPosition);
      startY += lineHeight; // Move down for request body
      doc.setFont("helvetica", "normal");
      if (checkPageHeight(requestLines.length + 2)) addPage();
      doc.text(requestLines, margin, startY);

      // Add Response section only if there's a response
      if (response) {
        const responseYPosition = requestYPosition; // Start at the same Y position for the response
        const responseXPosition = (doc.internal.pageSize.getWidth() / 2) + margin; // Move to the right half for response
        const responseTitle = "Response:";
        const responseLines = [
          `Status Code: ${response.statusCode}`,
          `Message: ${response.message}`,
        ];

        // Draw response title
        doc.setFont("helvetica", "bold");
        doc.text(responseTitle, responseXPosition, responseYPosition);
        startY = requestYPosition; // Reset startY for response
        startY += lineHeight; // Move down for response content
        doc.setFont("helvetica", "normal");

        // Loop through response lines and print them
        responseLines.forEach((line, index) => {
          if (checkPageHeight(1)) addPage();
          doc.text(line, responseXPosition, startY + (lineHeight * index));
        });
        startY += lineHeight * (responseLines.length + 1); // Move Y position down for the next test case
      } else {
        startY += lineHeight * 2; // If no response, just move down for the next test case
      }

      // Check for page height after each test case and response
      if (checkPageHeight(4)) addPage(); // Adjust according to how many lines you expect for next test case
    });

    // Add page numbers
    const totalPages = doc.internal.getNumberOfPages();
    for (let i = 1; i <= totalPages; i++) {
      doc.setPage(i);
      doc.setFontSize(10);
      doc.text(`Page ${i} of ${totalPages}`, margin, pageHeight - margin);
    }

    // Save the PDF
    doc.save("TestResults.pdf");
  };


  const settingsRequest = {
    dots: false,
    infinite: true,
    speed: 500,
    slidesToShow: 1,
    slidesToScroll: 1,
    arrows: true,
    arrows: apiData.length > 0 && !loading,
    afterChange: (current) => setCurrentSlideRequest(current + 1),
  };

  const settings = {
    dots: false,
    infinite: true,
    speed: 500,
    slidesToShow: 1,
    slidesToScroll: 1,
    arrows: true,
    arrows: apiData.length > 0 && !loading,
    beforeChange: (oldIndex, newIndex) => {
      setCurrentSlide(newIndex);
    }
  };

  const syncSlide = (newIndex) => {
    setCurrentSlide(newIndex);


    if (requestSliderRef.current) {
      requestSliderRef.current.slickGoTo(newIndex);
    }
    if (responseSliderRef.current) {
      responseSliderRef.current.slickGoTo(newIndex);
    }
  };


  const handleCurlChange = (e) => {
    setCurl(e.target.value);
  };

  const handleDescriptionChange = (e) => {
    const value = e.target.value;
    if (value.length <= charLimit) {
      setDescription(value);
    }
  };

  const handleSubmit = async () => {
    setLoadingCurl(true);
    setFormattedResponse([]);
    setIsResponseShown(false);
    setShowFallbackImage(false);

    try {
      const requestBody = JSON.stringify({
        curl: curl,
        description: description
      });

      const response = await fetch('http://localhost:8090/requestFromCurl', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: requestBody
      });

      if (!response.ok) {
        const error = await response.json().catch(() => ({}));
        const errorMessage = error.message || 'Unable to parse the cURL';
        throw new Error(errorMessage);
      }

      const data = await response.json();
      const formattedData = Array.isArray(data) ? data.map((item, index) => ({
        id: index + 1,
        title: item.testCaseName || `Test Case ${index + 1}`,
        description: item.testRequestBody,
        validJSON: item.validJSON,
        isSelected: true
      })) : [{
        id: 1,
        title: data.testCaseName || 'Test Case 1',
        description: data.testRequestBody,
        validJSON: data.validJSON,
        isSelected: true
      }];

      setApiData(formattedData);
      setError(null);
    } catch (err) {
      setError(err.message);
      setShowFallbackImage(true);
      console.error("Error:", err);
    } finally {
      setLoadingCurl(false);
    }
  };


  const handleInputChange = (id, value) => {
    let isValidJson = true;

    try {
      JSON.parse(value);
    } catch (e) {
      isValidJson = false;
    }

    const updatedData = apiData.map((item) => {
      if (item.id === id) {
        return {
          ...item,
          description: value,
          isValidJson,
          hasUserEdited: true
        };
      }
      return item;
    });

    setApiData(updatedData);
  };

  const handleCheckboxChange = (id) => {
    const updatedData = apiData.map((item) => {
      if (item.id === id) {
        return { ...item, isSelected: !item.isSelected };
      }
      return item;
    });
    setApiData(updatedData);
  };

  const handleErrorToast = (errorMessage) => {
    createToast(errorMessage, {
      type: 'error',
    });
  };


  function findMessage(obj) {
    if (!obj || typeof obj !== 'object') return null;

    if (obj.message) return obj.message;


    for (let key in obj) {
      if (typeof obj[key] === 'object') {
        const foundMessage = findMessage(obj[key]);
        if (foundMessage) return foundMessage;
      }
    }

    return null;
  }
  const handleRunTestCase = async () => {
    setIsResponseShown(true);
    setIsSingleRequest(false);

    const selectedTestCases = apiData.filter(item => item.isSelected);

    if (selectedTestCases.length === 0) {
      console.error("No test cases selected");
      setToastMessage("No test cases selected");
      return;
    }

    setFormattedResponse([]);
    setLoading(true);

    setTimeout(() => {
      const bottomContainer = document.querySelector('.bottom-container');
      if (bottomContainer) {
        bottomContainer.scrollIntoView({ behavior: 'smooth' });
      }
    }, 300);

    try {
      const requestBody = {
        curl: curl,
        requestBodyList: selectedTestCases.map(item => ({
          tcId: item.id.toString(),
          testRequestBody: item.description
        }))
      };



      const response = await fetch('http://localhost:8090/runTestCase', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(requestBody)
      });

      if (!response.ok) {
        const error = await response.json().catch(() => ({}));
        const errorMessage = error.message || 'Failed to run test case';
        throw new Error(errorMessage);
      }

      const data = await response.json();
      const formattedResponse = data.map(item => {
        const tcResponse = JSON.parse(item.tcResponse);
        return {
          tcId: item.tcId,
          statusCode: item.statusCode,
          message: findMessage(tcResponse),
          // validationErrors: tcResponse.validationErrors,
        };
      });

      setFormattedResponse(formattedResponse);
      console.log('Test case run successfully:', formattedResponse);


    } catch (err) {
      console.error("Error running test case:", err.message);
      setToastMessage("Error running test case");
      handleErrorToast(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleIndividualSubmit = async (testCaseId) => {
    setIsSingleRequest(true);
    setIsResponseShown(true);
    const testCase = apiData.find(item => item.id === testCaseId);
    console.log(testCase);

    if (!testCase) {
      console.error("Test case not found");
      setToastMessage("Test case not found");
      return;
    }

    setFormattedResponse([]);
    setLoading(true);

    setTimeout(() => {
      const bottomContainer = document.querySelector('.bottom-container');
      if (bottomContainer) {
        bottomContainer.scrollIntoView({ behavior: 'smooth' });
      }
    }, 300);

    try {
      const requestBody = {
        curl: curl,
        requestBodyList: [{
          tcId: testCase.id.toString(),
          testRequestBody: testCase.description
        }]
      };

      const response = await fetch('http://localhost:8090/runTestCase', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(requestBody)
      });

      if (!response.ok) {
        const error = await response.json().catch(() => ({}));
        const errorMessage = error.message || 'Failed to run test case';
        throw new Error(errorMessage);
      }

      const data = await response.json();
      const formattedResponse = data.map(item => {
        const tcResponse = JSON.parse(item.tcResponse);
        return {
          tcId: item.tcId,
          statusCode: item.statusCode,
          message: tcResponse.message,
          validationErrors: tcResponse.validationErrors,
        };
      });

      setFormattedResponse(formattedResponse);
      console.log('Test case run successfully:', formattedResponse);

    } catch (err) {
      console.error("Error running test case:", err.message);
      setToastMessage("Error running test case");
      handleErrorToast(err.message);
    } finally {
      setLoading(false);
    }
  };




  return (
    <div className="outer-container">
      <div className="header">
        <h1 className='home-home-header'>AI Powered Sanity Checker</h1>
      </div>
      <div className="home-app-container">
        <div className="curl-section-left">
          <div className="input-container">
            <textarea
              className="curl-input"
              value={curl}
              onChange={handleCurlChange}
              placeholder="Enter cURL here"
              spellCheck={false}
            />
            <textarea
              className="description-input"
              value={description}
              onChange={handleDescriptionChange}
              placeholder="Please enter the description of API here for better response."
              maxLength={charLimit}
              spellCheck={false}
            />
            <button
              className={`submit-curl-btn ${loadingCurl ? 'loading' : ''}`}
              onClick={handleSubmit}
              disabled={isButtonDisabled}
            >
              Submit cURL and description
            </button>

          </div>
        </div>
        <div className='curl-section-right'>
          {loadingCurl ? (
            <div className='loader-container'>
              <div className="loader">
                <div className="box">
                  <div className="logo">
                    <svg viewBox="0 0 100 100" aria-hidden="true">
                      <path d="M100 34.2c-.4-2.6-3.3-4-5.3-5.3-3.6-2.4-7.1-4.7-10.7-7.1-8.5-5.7-17.1-11.4-25.6-17.1-2-1.3-4-2.7-6-4-1.4-1-3.3-1-4.8 0-5.7 3.8-11.5 7.7-17.2 11.5L5.2 29C3 30.4.1 31.8 0 34.8c-.1 3.3 0 6.7 0 10v16c0 2.9-.6 6.3 2.1 8.1 6.4 4.4 12.9 8.6 19.4 12.9 8 5.3 16 10.7 24 16 2.2 1.5 4.4 3.1 7.1 1.3 2.3-1.5 4.5-3 6.8-4.5 8.9-5.9 17.8-11.9 26.7-17.8l9.9-6.6c.6-.4 1.3-.8 1.9-1.3 1.4-1 2-2.4 2-4.1V37.3c.1-1.1.2-2.1.1-3.1 0-.1 0 .2 0 0zM54.3 12.3 88 34.8 73 44.9 54.3 32.4V12.3zm-8.6 0v20L27.1 44.8 12 34.8l33.7-22.5zM8.6 42.8 19.3 50 8.6 57.2V42.8zm37.1 44.9L12 65.2l15-10.1 18.6 12.5v20.1zM50 60.2 34.8 50 50 39.8 65.2 50 50 60.2zm4.3 27.5v-20l18.6-12.5 15 10.1-33.6 22.4zm37.1-30.5L80.7 50l10.8-7.2-.1 14.4z"></path>
                    </svg>
                  </div>
                </div>
                <div className="box"></div>
                <div className="box"></div>
                <div className="box"></div>
                <div className="box"></div>
              </div>
            </div>
          ) : showFallbackImage ? (
            <div className='fallback-image-container'>
              <img src={somethingWentWrong} alt="Fallback" />
              <p className="something-went-wrong-text">Oops! Something went wrong...</p>
              <p className="something-went-wrong-two">Please try refreshing the page</p>
              <button
                className="refresh-btn"
                onClick={() => window.location.reload()}
              >
                Refresh
              </button>
            </div>
          ) : apiData.length === 0 ? (
            <div className="empty-image">
              <img src={noRequestsImage} alt="No Requests" className="empty-image" />
              <p className="no-requests-text">Looks like we don’t have any requests right now!</p>
              <p className="no-requests-text-two">To see the requests, try entering the curl and its description.</p>
            </div>
          ) : (
            <div className="carousel-container">
              <Slider {...settingsRequest}>
                {apiData.map((formattedData) => (
                  <div key={formattedData.id} className="carousel-card">
                    <div className="card-header">
                      <label>
                        <input
                          type="checkbox"
                          className='checkbox'
                          checked={formattedData.isSelected}
                          onChange={() => handleCheckboxChange(formattedData.id)}
                        />
                        <span className='label-checkbox'>Test Case {formattedData.id}</span>
                      </label>
                      <span className={formattedData.hasUserEdited
                        ? (formattedData.isValidJson === false ? 'invalid-json-text' : 'valid-json-text')
                        : (formattedData.validJSON ? 'valid-json-text' : 'invalid-json-text')}>
                        {formattedData.hasUserEdited
                          ? (formattedData.isValidJson === false ? 'Invalid JSON' : 'Valid JSON')
                          : (formattedData.validJSON ? 'Valid JSON' : 'Invalid JSON')}
                      </span>
                      <button
                        className="submit-button"
                        onClick={() => handleIndividualSubmit(formattedData.id)}
                      >
                        Submit
                      </button>
                    </div>

                    <h2 className='home-card-heading'>{formattedData.title}</h2>
                    <textarea
                      className={`json-editor ${formattedData.isValidJson === false ? 'invalid-json' : formattedData.isValidJson === true ? 'valid-json' : ''}`}
                      value={formattedData.description}
                      onChange={(e) => handleInputChange(formattedData.id, e.target.value)}
                      rows={10}
                      spellCheck="false"
                    />
                  </div>
                ))}
              </Slider>
            </div>
          )}
          {!loadingCurl && (
            <div className="slide-count">
              {apiData.length > 0 && (
                <>
                  {`${currentSlideRequest} / ${apiData.length}`}
                  <div className='home-Bottom-container'>
                    <button
                      className="additional-button"
                      onClick={handleRunTestCase}
                    >
                      Submit Requests
                    </button>
                  </div>
                </>
              )}
            </div>
          )}
        </div>
      </div>
      {!loadingCurl && isResponseShown && (
        <hr className="divider" />
      )}
      <div>
        {!loadingCurl && isResponseShown && (
          <div className='bottom-container'>
            <div className='bottom-container-heading'>
              <h2 className='centered-heading'>Requests & Responses</h2>
            </div>
            {loading && (
              <div className='bottom-loader-container'>
                <div className="loader">
                  <div className="box">
                    <div className="logo">
                      <svg viewBox="0 0 100 100" aria-hidden="true">
                        <path d="M100 34.2c-.4-2.6-3.3-4-5.3-5.3-3.6-2.4-7.1-4.7-10.7-7.1-8.5-5.7-17.1-11.4-25.6-17.1-2-1.3-4-2.7-6-4-1.4-1-3.3-1-4.8 0-5.7 3.8-11.5 7.7-17.2 11.5L5.2 29C3 30.4.1 31.8 0 34.8c-.1 3.3 0 6.7 0 10v16c0 2.9-.6 6.3 2.1 8.1 6.4 4.4 12.9 8.6 19.4 12.9 8 5.3 16 10.7 24 16 2.2 1.5 4.4 3.1 7.1 1.3 2.3-1.5 4.5-3 6.8-4.5 8.9-5.9 17.8-11.9 26.7-17.8l9.9-6.6c.6-.4 1.3-.8 1.9-1.3 1.4-1 2-2.4 2-4.1V37.3c.1-1.1.2-2.1.1-3.1 0-.1 0 .2 0 0zM54.3 12.3 88 34.8 73 44.9 54.3 32.4V12.3zm-8.6 0v20L27.1 44.8 12 34.8l33.7-22.5zM8.6 42.8 19.3 50 8.6 57.2V42.8zm37.1 44.9L12 65.2l15-10.1 18.6 12.5v20.1zM50 60.2 34.8 50 50 39.8 65.2 50 50 60.2zm4.3 27.5v-20l18.6-12.5 15 10.1-33.6 22.4zm37.1-30.5L80.7 50l10.8-7.2-.1 14.4z"></path>
                      </svg>
                    </div>
                  </div>
                  <div className="box"></div>
                  <div className="box"></div>
                  <div className="box"></div>
                  <div className="box"></div>
                </div>
              </div>
            )}
            {!loading && (
              <div className='bottom-container-content'>
                <div className="bottom-container-left">
                  <div className="request-carousel-container">
                    <Slider
                      {...{
                        ...settings,
                        slidesToShow: 1,
                        slidesToScroll: 1,
                        infinite: false,
                        arrows: !isSingleRequest,
                      }}
                      ref={requestSliderRef}
                      beforeChange={(oldIndex, newIndex) => syncSlide(newIndex)}
                    >
                      {apiData.map((formattedData) => (
                        <div key={formattedData.id} className="request-carousel-card">
                          <div className="card-header">
                            <label>
                              <span className="label-checkbox">
                                Test Case {formattedData.id}
                              </span>
                            </label>
                            <span
                              className={
                                formattedData.hasUserEdited
                                  ? formattedData.isValidJson === false
                                    ? 'invalid-json-text'
                                    : 'valid-json-text'
                                  : formattedData.validJSON
                                    ? 'valid-json-text'
                                    : 'invalid-json-text'
                              }
                            >
                              {formattedData.hasUserEdited
                                ? formattedData.isValidJson === false
                                  ? 'Invalid JSON'
                                  : 'Valid JSON'
                                : formattedData.validJSON
                                  ? 'Valid JSON'
                                  : 'Invalid JSON'}
                            </span>
                          </div>

                          <h2 className="home-card-heading">{formattedData.title}</h2>
                          <textarea
                            className="request-json-editor"
                            value={formattedData.description}
                            rows={10}
                            spellCheck="false"
                            disabled
                          />
                        </div>
                      ))}
                    </Slider>
                  </div>
                </div>


                <div className="bottom-container-right">
                  <div className="response-carousel-container">
                    <Slider
                      {...{
                        ...settings,
                        slidesToShow: 1,
                        slidesToScroll: 1,
                        infinite: false,
                        arrows: formattedResponse.length > 1,
                      }}
                      ref={responseSliderRef}
                      beforeChange={(oldIndex, newIndex) => syncSlide(newIndex)}
                    >
                      {formattedResponse.map((item) => (
                        <div key={item.tcId} className="response-carousel-card">
                          <h3 className='label-checkbox'>Test Case: {item.tcId}</h3>
                          <p className='status-code'>Status Code: {item.statusCode}</p>
                          {/* <p className='response-message'>Message: {item.message}</p> */}
                          <textarea
                            className="request-json-editor"
                            value={`Message: ${item.message}`}
                            rows={10}
                            spellCheck="false"
                            disabled
                          />
                        </div>
                      ))}
                    </Slider>
                  </div>
                  <button className="floating-download-btn" onClick={handleDownloadPDF}>
                    Download PDF
                  </button>
                </div>
              </div>
            )}
          </div>
        )}
      </div>

      {formattedResponse.some(item => item.validationErrors && item.validationErrors.length > 0) &&
        handleErrorToast("Error running test case")
      }
    </div>
  );
};

export default Home;
