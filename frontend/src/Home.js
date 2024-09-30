import React, { useState } from 'react';
import Slider from "react-slick";
import './Home.css';

const Home = () => {
  const [curl, setCurl] = useState('');
  const [description, setDescription] = useState('');
  const [apiData, setApiData] = useState([]);
  const [error, setError] = useState(null);
  const [currentSlide, setCurrentSlide] = useState(1);

  const settings = {
    dots: false,
    infinite: true,
    speed: 500,
    slidesToShow: 1,
    slidesToScroll: 1,
    arrows: true,
    afterChange: (current) => setCurrentSlide(current + 1),
    //    centerMode: true,
    //    centerPadding: '10%',
  };

  const handleCurlChange = (e) => {
    setCurl(e.target.value);
  };

  const handleDescriptionChange = (e) => {
    setDescription(e.target.value);
  };

  const handleSubmit = async () => {
    console.log('cURL:', curl);
    console.log('Description:', description);
    try {
      const response = await fetch('http://localhost:8090/requestFromCurl', {
        method: 'POST',
        headers: {
          'Content-Type': 'text/plain',
        },
        body: curl
      });

      if (!response.ok) {
        const error = await response.json().catch(() => ({}));
        const errorMessage = error.message || 'Unable to parse the cURL';
        throw new Error(errorMessage);
      }

      const data = await response.json();

      // Convert the API response to a format usable in cards
      const formattedData = Object.keys(data).map((key, index) => ({
        id: index + 1,
        title: key.replace(/\"/g, ''), // Clean up the key by removing extra quotes
        description: JSON.stringify(JSON.parse(data[key]), null, 2), // Parse and prettify the JSON data
      }));
      console.log(formattedData);

      setApiData(formattedData); // Store the formatted data in state
      setError(null); // Clear any previous errors
    } catch (err) {
      setError(err.message);
      console.error("Error:", err);
    }
  };

  const handleInputChange = (id, key, value) => {
    const updatedData = apiData.map((item) => {
      if (item.id === id) {
        const updatedDescription = { ...JSON.parse(item.description), [key]: value };
        return { ...item, description: JSON.stringify(updatedDescription, null, 2) };
      }
      return item;
    });
    setApiData(updatedData);
  };

  return (
    <div class="outer-container">
      <div className="home-app-container">
        <div className="curl-section-left">
          <div className="input-container">
            <textarea
              className="curl-input"
              value={curl}
              onChange={handleCurlChange}
              placeholder="Enter cURL here"
            />
            <button className="submit-curl-btn" onClick={handleSubmit}>
              Submit curl
            </button>
            <textarea
              className="description-input"
              value={description}
              onChange={handleDescriptionChange}
              placeholder="Enter description of API here"
            />
            <button className="submit-btn" onClick={handleSubmit}>
              Submit Description
            </button>
          </div>
        </div>
        <div className='curl-section-right'>
          <div className="carousel-container">
            <Slider {...settings}>
              {apiData.map((formattedData) => {
                const parsedDescription = JSON.parse(formattedData.description);
                return (
                  <div key={formattedData.id} className="carousel-card">
                    <label>
                      <input type="checkbox" className='checkbox' />
                      <span className='label-checkbox'>Test Case {formattedData.id}</span>
                    </label>
                    <h2>{formattedData.title}</h2>
                    <ul className="description-list">
                      {Object.keys(parsedDescription).map((key) => (
                        <li key={key} className="description-item">
                          <label>
                            <input type="checkbox" className="description-checkbox" />
                            <span className="key-name">{key}:</span>
                            <input
                              type="text"
                              className="value-input"
                              value={parsedDescription[key]}
                              onChange={(e) => handleInputChange(formattedData.id, key, e.target.value)}
                            />
                          </label>
                        </li>
                      ))}
                    </ul>
                  </div>
                )
              })}
            </Slider>
          </div>
          <div className="slide-count">
            {currentSlide} / {apiData.length}
          </div>
        </div>
      </div>
    </div>
  );
};

export default Home;
